package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/contracts")
@RequiredArgsConstructor
public class ContractAdminController {
    private final ContractMapper contractMapper;
    private final QuoteMapper quoteMapper;
    private final CustomerMapper customerMapper;
    private final ContractTemplateMapper templateMapper;
    private final SalesOrderMapper orderMapper;
    private final PaymentPlanMapper paymentMapper;
    private final InvoiceMapper invoiceMapper;
    private final InstallationMapper installationMapper;
    private final EquipmentMapper equipmentMapper;
    private final MaintenancePlanMapper maintenanceMapper;
    private final DemandMapper demandMapper;
    private final TenantMapper tenantMapper;
    private final IdGenerator idGenerator;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final PdfService pdfService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ApiResult<PageResult<Contract>> list(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String auditStatus,
                                                @RequestParam(required = false) Long customerId) {
        var user = AuthContext.require();
        Page<Contract> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Contract> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) qw.and(w -> w.like(Contract::getContractNo, keyword)
                .or().like(Contract::getCustomerName, keyword).or().like(Contract::getTitle, keyword));
        if (status != null && !status.isBlank()) qw.eq(Contract::getStatus, status);
        if (auditStatus != null && !auditStatus.isBlank()) qw.eq(Contract::getAuditStatus, auditStatus);
        if (customerId != null) qw.eq(Contract::getCustomerId, customerId);
        qw.orderByDesc(Contract::getId);
        return ApiResult.ok(PageResult.of(contractMapper.selectPage(p, qw)));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Contract contract = find(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contract", contract);
        result.put("quote", contract.getQuoteId() == null ? null : quoteMapper.selectById(contract.getQuoteId()));
        result.put("customer", contract.getCustomerId() == null ? null : customerMapper.selectById(contract.getCustomerId()));
        result.put("order", orderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getContractId, id).last("limit 1")));
        result.put("payments", paymentMapper.selectList(new LambdaQueryWrapper<PaymentPlan>()
                .eq(PaymentPlan::getContractId, id).orderByAsc(PaymentPlan::getId)));
        result.put("invoices", invoiceMapper.selectList(new LambdaQueryWrapper<Invoice>()
                .eq(Invoice::getContractId, id).orderByDesc(Invoice::getId)));
        result.put("installations", installationMapper.selectList(new LambdaQueryWrapper<Installation>()
                .eq(Installation::getContractId, id).orderByAsc(Installation::getId)));
        result.put("equipment", equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getContractId, id)));
        result.put("templates", templateMapper.selectList(new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getStatus, "NORMAL")));
        return ApiResult.ok(result);
    }

    @PostMapping
    public ApiResult<Contract> create(@RequestBody Contract contract) {
        var user = AuthContext.require();
        contract.setId(null);
        contract.setTenantId(user.getTenantId());
        contract.setContractNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "CONTRACT"));
        contract.setAuditStatus("PENDING");
        contract.setSignStatus("UNSIGNED");
        contract.setStatus("DRAFT");
        contract.setOwnerId(user.getId());
        if (contract.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(contract.getCustomerId());
            if (customer != null) contract.setCustomerName(customer.getName());
        }
        contractMapper.insert(contract);
        auditService.log("合同履约", "新增合同", contract.getContractNo(), contract.getTitle());
        return ApiResult.ok(contract);
    }

    @PutMapping("/{id}")
    public ApiResult<Contract> update(@PathVariable Long id, @RequestBody Contract contract) {
        Contract existing = find(id);
        if ("SIGNED".equals(existing.getSignStatus())) throw new BizException("已签署合同不可编辑");
        contract.setId(id);
        contract.setTenantId(existing.getTenantId());
        contract.setContractNo(existing.getContractNo());
        contract.setStatus(existing.getStatus());
        contract.setAuditStatus(existing.getAuditStatus());
        contract.setSignStatus(existing.getSignStatus());
        contract.setOwnerId(existing.getOwnerId());
        contractMapper.updateById(contract);
        auditService.log("合同履约", "修改合同", existing.getContractNo(), null);
        return ApiResult.ok(contractMapper.selectById(id));
    }

    @PostMapping("/{id}/approve")
    public ApiResult<Contract> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var user = AuthContext.require();
        if (!user.isAdmin()) throw new BizException(403, "仅管理员可审批合同");
        Contract contract = find(id);
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", false)));
        contract.setAuditStatus(approved ? "APPROVED" : "REJECTED");
        contract.setStatus(approved ? "APPROVED" : "DRAFT");
        contractMapper.updateById(contract);
        auditService.log("合同履约", approved ? "合同审批通过" : "合同审批驳回", contract.getContractNo(),
                String.valueOf(body.getOrDefault("comment", "")));
        return ApiResult.ok(contract);
    }

    @PostMapping("/{id}/sign")
    public ApiResult<Contract> sign(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var user = AuthContext.require();
        Contract contract = find(id);
        if (!"APPROVED".equals(contract.getAuditStatus())) throw new BizException("合同审批通过后方可签署");
        contract.setSignStatus("SIGNED");
        contract.setSignType(body.getOrDefault("signType", "ELECTRONIC"));
        contract.setSignTime(LocalDateTime.now());
        contract.setStatus("PERFORMING");
        contractMapper.updateById(contract);
        ensureOrder(contract, user);
        ensurePayments(contract);
        ensureInstallations(contract);
        if (contract.getQuoteId() != null) {
            Quote quote = quoteMapper.selectById(contract.getQuoteId());
            if (quote != null && quote.getDemandId() != null) {
                Demand demand = demandMapper.selectById(quote.getDemandId());
                if (demand != null) {
                    demand.setStatus("DEAL");
                    demandMapper.updateById(demand);
                }
            }
        }
        if (contract.getCustomerId() != null) {
            notificationService.notifyCustomer(user.getTenantId(), contract.getCustomerId(), "DEAL_NOTICE",
                    "合同已成交，感谢您的信任", "合同 " + contract.getContractNo() + " 已签署，金额 "
                            + contract.getAmount() + " 元", Map.of("contractNo", contract.getContractNo(),
                            "amount", String.valueOf(contract.getAmount()), "paymentProgress", "已启动收款计划"),
                    "/account");
        }
        auditService.log("合同履约", "合同签署", contract.getContractNo(), contract.getSignType());
        return ApiResult.ok(contract);
    }

    @PostMapping("/{id}/archive")
    public ApiResult<Contract> archive(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Contract contract = find(id);
        contract.setArchiveUrl(body.get("archiveUrl"));
        contractMapper.updateById(contract);
        auditService.log("合同履约", "合同归档", contract.getContractNo(), contract.getArchiveUrl());
        return ApiResult.ok(contract);
    }

    @GetMapping("/{id}/payment-plans")
    public ApiResult<List<PaymentPlan>> paymentPlans(@PathVariable Long id) {
        find(id);
        return ApiResult.ok(paymentMapper.selectList(new LambdaQueryWrapper<PaymentPlan>()
                .eq(PaymentPlan::getContractId, id).orderByAsc(PaymentPlan::getId)));
    }

    @PostMapping("/{id}/payment-plans")
    public ApiResult<PaymentPlan> addPaymentPlan(@PathVariable Long id, @RequestBody PaymentPlan plan) {
        Contract contract = find(id);
        plan.setId(null);
        plan.setTenantId(contract.getTenantId());
        plan.setContractId(id);
        if (plan.getStatus() == null) plan.setStatus("UNPAID");
        paymentMapper.insert(plan);
        auditService.log("合同履约", "新增收款计划", contract.getContractNo(), plan.getPhase());
        return ApiResult.ok(plan);
    }

    @PostMapping("/payment-plans/{id}/receive")
    public ApiResult<PaymentPlan> receive(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var user = AuthContext.require();
        PaymentPlan plan = paymentMapper.selectById(id);
        if (plan == null) throw new BizException(404, "收款计划不存在");
        BigDecimal amount = new BigDecimal(String.valueOf(body.getOrDefault("amount", plan.getAmount())));
        plan.setReceivedAmount(amount);
        plan.setReceivedDate(LocalDate.now());
        plan.setStatus("PAID");
        paymentMapper.updateById(plan);
        auditService.log("回款", "登记回款", String.valueOf(id), "金额 " + amount);
        return ApiResult.ok(plan);
    }

    @GetMapping("/{id}/invoices")
    public ApiResult<List<Invoice>> invoices(@PathVariable Long id) {
        find(id);
        return ApiResult.ok(invoiceMapper.selectList(new LambdaQueryWrapper<Invoice>()
                .eq(Invoice::getContractId, id).orderByDesc(Invoice::getId)));
    }

    @PostMapping("/{id}/invoices")
    public ApiResult<Invoice> createInvoice(@PathVariable Long id, @RequestBody Invoice invoice) {
        Contract contract = find(id);
        invoice.setId(null);
        invoice.setTenantId(contract.getTenantId());
        invoice.setContractId(id);
        if (invoice.getStatus() == null) invoice.setStatus("ISSUED");
        if (invoice.getIssuedAt() == null) invoice.setIssuedAt(LocalDate.now());
        invoiceMapper.insert(invoice);
        auditService.log("发票", "开具", contract.getContractNo(), invoice.getInvoiceNo());
        return ApiResult.ok(invoice);
    }

    @PostMapping("/invoices/{id}/void")
    public ApiResult<Invoice> voidInvoice(@PathVariable Long id) {
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null) throw new BizException(404, "发票不存在");
        invoice.setStatus("VOID");
        invoiceMapper.updateById(invoice);
        auditService.log("发票", "作废", String.valueOf(id), invoice.getInvoiceNo());
        return ApiResult.ok(invoice);
    }

    @GetMapping("/{id}/installations")
    public ApiResult<List<Installation>> installations(@PathVariable Long id) {
        find(id);
        return ApiResult.ok(installationMapper.selectList(new LambdaQueryWrapper<Installation>()
                .eq(Installation::getContractId, id).orderByAsc(Installation::getId)));
    }

    @PostMapping("/{id}/installations/{installId}/update")
    public ApiResult<Installation> updateInstallation(@PathVariable Long id, @PathVariable Long installId,
                                                       @RequestBody Installation installation) {
        Contract contract = find(id);
        Installation existing = installationMapper.selectById(installId);
        if (existing == null || !existing.getContractId().equals(id)) throw new BizException(404, "履约节点不存在");
        installation.setId(installId);
        installation.setTenantId(contract.getTenantId());
        installation.setContractId(id);
        installationMapper.updateById(installation);
        if ("ACCEPTANCE".equals(existing.getStage()) && "DONE".equals(installation.getStatus())) {
            completeAcceptance(contract, installation);
        }
        notificationService.notifyCustomer(contract.getTenantId(), contract.getCustomerId(), "DELIVERY_NOTICE",
                "履约进度更新", "合同 " + contract.getContractNo() + "：" + existing.getStage() + " "
                        + (installation.getStatus() == null ? "" : installation.getStatus()),
                Map.of("contractNo", contract.getContractNo(), "stage", existing.getStage(),
                        "progress", String.valueOf(installation.getStatus())), "/account");
        auditService.log("安装履约", "节点更新", contract.getContractNo(), existing.getStage());
        return ApiResult.ok(installation);
    }

    @GetMapping("/{id}/pdf")
    public void pdf(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Contract contract = find(id);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("合同编号", contract.getContractNo());
        fields.put("客户", contract.getCustomerName());
        fields.put("合同金额", "¥" + contract.getAmount());
        fields.put("付款条件", contract.getPaymentTerms());
        fields.put("审批状态", contract.getAuditStatus());
        fields.put("签署状态", contract.getSignStatus());
        fields.put("签署时间", String.valueOf(contract.getSignTime()));
        List<String[]> table = new ArrayList<>();
        table.add(new String[]{"收款期次", "金额", "计划日期", "实收日期", "状态"});
        for (PaymentPlan plan : paymentMapper.selectList(new LambdaQueryWrapper<PaymentPlan>()
                .eq(PaymentPlan::getContractId, id))) {
            table.add(new String[]{plan.getPhase(), String.valueOf(plan.getAmount()), String.valueOf(plan.getPlanDate()),
                    String.valueOf(plan.getReceivedDate()), plan.getStatus()});
        }
        byte[] bytes = pdfService.build("锅炉设备采购合同", contract.getTitle(), fields, table,
                List.of("合同正文详见附件；锅炉制造许可、产品监检证明、能效报告作为合同附件。",
                        "安装须由具备安装许可资质的单位实施，并按TSG 11完成检验。"),
                "甲方（盖章）：______________________        乙方（盖章）：______________________");
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                java.net.URLEncoder.encode(contract.getContractNo() + ".pdf", java.nio.charset.StandardCharsets.UTF_8));
        response.getOutputStream().write(bytes);
        auditService.log("合同履约", "PDF下载", contract.getContractNo(), null);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResult<Void> delete(@PathVariable Long id) {
        Contract contract = find(id);
        List<Long> equipmentIds = jdbcTemplate.queryForList(
                "SELECT id FROM equipment WHERE contract_id = ?", Long.class, id);
        if (!equipmentIds.isEmpty()) {
            String placeholders = equipmentIds.stream().map(x -> "?").collect(java.util.stream.Collectors.joining(","));
            Object[] args = equipmentIds.toArray();
            jdbcTemplate.update("DELETE FROM maintenance_plan WHERE equipment_id IN (" + placeholders + ")", args);
            jdbcTemplate.update("UPDATE service_ticket SET equipment_id = NULL WHERE equipment_id IN (" + placeholders + ")", args);
            jdbcTemplate.update("UPDATE certificate SET equipment_id = NULL WHERE equipment_id IN (" + placeholders + ")", args);
        }
        jdbcTemplate.update("DELETE FROM satisfaction WHERE contract_id = ?", id);
        jdbcTemplate.update("DELETE FROM customer_visit WHERE contract_id = ?", id);
        jdbcTemplate.update("DELETE FROM payment_plan WHERE contract_id = ?", id);
        jdbcTemplate.update("DELETE FROM invoice WHERE contract_id = ?", id);
        jdbcTemplate.update("DELETE FROM sales_order WHERE contract_id = ?", id);
        jdbcTemplate.update("DELETE FROM installation WHERE contract_id = ?", id);
        jdbcTemplate.update("DELETE FROM equipment WHERE contract_id = ?", id);
        jdbcTemplate.update("DELETE FROM contract WHERE id = ?", id);
        auditService.log("合同履约", "删除", contract.getContractNo(), contract.getTitle());
        return ApiResult.ok();
    }

    private Contract find(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) throw new BizException(404, "合同不存在");
        return contract;
    }

    private void ensureOrder(Contract contract, AuthUser user) {
        if (orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getContractId, contract.getId())) > 0) return;
        SalesOrder order = new SalesOrder();
        order.setTenantId(contract.getTenantId());
        order.setOrderNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "ORDER"));
        order.setContractId(contract.getId());
        order.setProductionProgress("0%");
        order.setDeliveryStatus("PENDING");
        order.setExpectedDelivery(LocalDate.now().plusDays(45));
        orderMapper.insert(order);
    }

    private void ensurePayments(Contract contract) {
        if (paymentMapper.selectCount(new LambdaQueryWrapper<PaymentPlan>()
                .eq(PaymentPlan::getContractId, contract.getId())) > 0) return;
        BigDecimal total = contract.getAmount() == null ? BigDecimal.ZERO : contract.getAmount();
        BigDecimal deposit = total.multiply(new BigDecimal("0.3")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = total.multiply(new BigDecimal("0.4")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = total.subtract(deposit).subtract(shipping);
        addPlan(contract, "定金", deposit, LocalDate.now());
        addPlan(contract, "发货款", shipping, LocalDate.now().plusDays(30));
        addPlan(contract, "验收尾款", finalAmount, LocalDate.now().plusDays(75));
    }

    private void addPlan(Contract contract, String phase, BigDecimal amount, LocalDate planDate) {
        PaymentPlan plan = new PaymentPlan();
        plan.setTenantId(contract.getTenantId());
        plan.setContractId(contract.getId());
        plan.setPhase(phase);
        plan.setAmount(amount);
        plan.setPlanDate(planDate);
        plan.setReceivedAmount(BigDecimal.ZERO);
        plan.setStatus("UNPAID");
        paymentMapper.insert(plan);
    }

    private void ensureInstallations(Contract contract) {
        if (installationMapper.selectCount(new LambdaQueryWrapper<Installation>()
                .eq(Installation::getContractId, contract.getId())) > 0) return;
        for (String stage : List.of("SHIPPED", "ARRIVED", "INSTALL", "ACCEPTANCE")) {
            Installation installation = new Installation();
            installation.setTenantId(contract.getTenantId());
            installation.setContractId(contract.getId());
            installation.setStage(stage);
            installation.setStatus("PENDING");
            installation.setPlanDate(LocalDate.now().plusDays(
                    switch (stage) {
                        case "SHIPPED" -> 30;
                        case "ARRIVED" -> 35;
                        case "INSTALL" -> 45;
                        default -> 60;
                    }));
            installationMapper.insert(installation);
        }
    }

    private void completeAcceptance(Contract contract, Installation acceptance) {
        if (equipmentMapper.selectCount(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getContractId, contract.getId())) > 0) return;
        Quote quote = contract.getQuoteId() == null ? null : quoteMapper.selectById(contract.getQuoteId());
        Product product = quote == null || quote.getProductId() == null ? null : null;
        Tenant tenant = tenantMapper.selectById(contract.getTenantId());
        Equipment equipment = new Equipment();
        equipment.setTenantId(contract.getTenantId());
        equipment.setEquipmentNo(idGenerator.next(contract.getTenantId(), tenant == null ? "T" : tenant.getCode(), "EQUIPMENT"));
        equipment.setCustomerId(contract.getCustomerId());
        equipment.setContractId(contract.getId());
        equipment.setProductId(quote == null ? null : quote.getProductId());
        equipment.setModel(quote == null ? null : quote.getProductName());
        equipment.setInstallAddr(acceptance.getRemark());
        equipment.setInstallDate(acceptance.getActualDate() == null ? LocalDate.now() : acceptance.getActualDate());
        equipment.setWarrantyEnd(LocalDate.now().plusMonths(12));
        equipment.setStatus("ACTIVE");
        equipmentMapper.insert(equipment);
        addMaintenance(equipment, "MAINTAIN", 6);
        addMaintenance(equipment, "ANNUAL", 12);
        addMaintenance(equipment, "PERIODIC", 24);
        auditService.log("安装履约", "验收完成并建档", contract.getContractNo(), equipment.getEquipmentNo());
    }

    private void addMaintenance(Equipment equipment, String project, int months) {
        MaintenancePlan plan = new MaintenancePlan();
        plan.setTenantId(equipment.getTenantId());
        plan.setEquipmentId(equipment.getId());
        plan.setEquipmentNo(equipment.getEquipmentNo());
        plan.setCustomerId(equipment.getCustomerId());
        plan.setProject(project);
        plan.setCycleMonths(months);
        plan.setNextDate(LocalDate.now().plusMonths(months));
        plan.setStatus("ACTIVE");
        plan.setRemindDays(30);
        maintenanceMapper.insert(plan);
    }
}
