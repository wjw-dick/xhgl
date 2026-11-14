package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class CustomerAdminController {
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final DemandMapper demandMapper;
    private final ConsultationMapper consultationMapper;
    private final SolutionMapper solutionMapper;
    private final QuoteMapper quoteMapper;
    private final ContractMapper contractMapper;
    private final PaymentPlanMapper paymentPlanMapper;
    private final ServiceTicketMapper ticketMapper;
    private final EquipmentMapper equipmentMapper;
    private final CertificateMapper certificateMapper;
    private final ReferralMapper referralMapper;
    private final CustomerVisitMapper visitMapper;
    private final IdGenerator idGenerator;
    private final CryptoService cryptoService;
    private final AuditService auditService;
    private final ExcelService excelService;
    private final RateLimiter rateLimiter;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ApiResult<PageResult<Customer>> list(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String industry,
                                                @RequestParam(required = false) Long ownerId) {
        Page<Customer> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Customer::getName, keyword)
                    .or().like(Customer::getCustomerNo, keyword)
                    .or().like(Customer::getAddress, keyword));
        }
        if (status != null && !status.isBlank()) qw.eq(Customer::getStatus, status);
        if (industry != null && !industry.isBlank()) qw.eq(Customer::getIndustry, industry);
        if (ownerId != null) qw.eq(Customer::getOwnerId, ownerId);
        qw.orderByDesc(Customer::getId);
        return ApiResult.ok(PageResult.of(customerMapper.selectPage(p, qw)));
    }

    @GetMapping("/options")
    public ApiResult<List<Customer>> options() {
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<Customer>().eq(Customer::getStatus, "NORMAL");
        qw.orderByDesc(Customer::getId).last("limit 200");
        return ApiResult.ok(customerMapper.selectList(qw));
    }

    @GetMapping("/{id}")
    public ApiResult<Customer> detail(@PathVariable Long id) {
        return ApiResult.ok(check(id));
    }

    @PostMapping
    public ApiResult<Customer> create(@RequestBody Customer customer) {
        var user = AuthContext.require();
        customer.setId(null);
        customer.setTenantId(user.getTenantId());
        customer.setCustomerNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "CUSTOMER"));
        if (customer.getStatus() == null) customer.setStatus("NORMAL");
        if (customer.getOwnerId() == null) customer.setOwnerId(user.getId());
        customerMapper.insert(customer);
        auditService.log("客户档案", "新增", customer.getCustomerNo(), customer.getName());
        return ApiResult.ok(customer);
    }

    @PutMapping("/{id}")
    public ApiResult<Customer> update(@PathVariable Long id, @RequestBody Customer customer) {
        Customer existing = check(id);
        customer.setId(id);
        customer.setTenantId(existing.getTenantId());
        customer.setCustomerNo(existing.getCustomerNo());
        if (customer.getOwnerId() == null) customer.setOwnerId(existing.getOwnerId());
        customerMapper.updateById(customer);
        auditService.log("客户档案", "修改", existing.getCustomerNo(), customer.getName());
        return ApiResult.ok(customerMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResult<Void> delete(@PathVariable Long id) {
        Customer existing = check(id);
        if (hasBusinessData(id)) {
            throw new BizException(409, "该客户已关联业务数据，无法直接删除；请先处理相关业务或改为禁用");
        }
        jdbcTemplate.update("DELETE FROM notification WHERE customer_id = ?", id);
        jdbcTemplate.update("DELETE FROM notify_log WHERE customer_id = ?", id);
        jdbcTemplate.update("DELETE FROM tenant_user WHERE customer_id = ? AND role_code = 'CUSTOMER'", id);
        jdbcTemplate.update("DELETE FROM contact WHERE customer_id = ?", id);
        jdbcTemplate.update("DELETE FROM customer WHERE id = ?", id);
        auditService.log("客户档案", "删除", existing.getCustomerNo(), existing.getName());
        return ApiResult.ok();
    }

    private boolean hasBusinessData(Long customerId) {
        return demandMapper.selectCount(new LambdaQueryWrapper<Demand>().eq(Demand::getCustomerId, customerId)) > 0
                || consultationMapper.selectCount(new LambdaQueryWrapper<Consultation>().eq(Consultation::getCustomerId, customerId)) > 0
                || quoteMapper.selectCount(new LambdaQueryWrapper<Quote>().eq(Quote::getCustomerId, customerId)) > 0
                || contractMapper.selectCount(new LambdaQueryWrapper<Contract>().eq(Contract::getCustomerId, customerId)) > 0
                || ticketMapper.selectCount(new LambdaQueryWrapper<ServiceTicket>().eq(ServiceTicket::getCustomerId, customerId)) > 0
                || equipmentMapper.selectCount(new LambdaQueryWrapper<Equipment>().eq(Equipment::getCustomerId, customerId)) > 0
                || certificateMapper.selectCount(new LambdaQueryWrapper<Certificate>().eq(Certificate::getCustomerId, customerId)) > 0
                || referralMapper.selectCount(new LambdaQueryWrapper<Referral>().eq(Referral::getSourceCustomerId, customerId)) > 0
                || visitMapper.selectCount(new LambdaQueryWrapper<CustomerVisit>().eq(CustomerVisit::getCustomerId, customerId)) > 0;
    }

    @PostMapping("/{id}/disable")
    public ApiResult<Void> disable(@PathVariable Long id) {
        Customer customer = check(id);
        customer.setStatus("DISABLED");
        customerMapper.updateById(customer);
        auditService.log("客户档案", "禁用", customer.getCustomerNo(), null);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/contacts")
    public ApiResult<List<Contact>> contacts(@PathVariable Long id) {
        check(id);
        return ApiResult.ok(contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, id).orderByDesc(Contact::getPrimaryFlag).orderByAsc(Contact::getId)));
    }

    @PostMapping("/{id}/contacts")
    public ApiResult<Contact> addContact(@PathVariable Long id, @RequestBody Contact contact) {
        Customer customer = check(id);
        contact.setId(null);
        contact.setTenantId(customer.getTenantId());
        contact.setCustomerId(id);
        encryptPhone(contact);
        if (contact.getPhoneMask() != null && !contact.getPhoneMask().isBlank()) {
            Contact duplicate = contactMapper.selectOne(new LambdaQueryWrapper<Contact>()
                    .eq(Contact::getCustomerId, id)
                    .eq(Contact::getName, contact.getName())
                    .eq(Contact::getPhoneMask, contact.getPhoneMask())
                    .last("limit 1"));
            if (duplicate != null) throw new BizException(409, "该联系人已存在");
        }
        contactMapper.insert(contact);
        auditService.log("客户档案", "新增联系人", customer.getCustomerNo(), contact.getName());
        return ApiResult.ok(contact);
    }

    @PutMapping("/{id}/contacts/{contactId}")
    public ApiResult<Contact> updateContact(@PathVariable Long id, @PathVariable Long contactId,
                                            @RequestBody Contact contact) {
        check(id);
        Contact existing = contactMapper.selectById(contactId);
        if (existing == null || !existing.getCustomerId().equals(id)) throw new BizException(404, "联系人不存在");
        contact.setId(contactId);
        contact.setTenantId(existing.getTenantId());
        contact.setCustomerId(id);
        encryptPhone(contact);
        contactMapper.updateById(contact);
        return ApiResult.ok(contact);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    public ApiResult<Void> deleteContact(@PathVariable Long id, @PathVariable Long contactId) {
        check(id);
        Contact existing = contactMapper.selectById(contactId);
        if (existing == null || !existing.getCustomerId().equals(id)) throw new BizException(404, "联系人不存在");
        contactMapper.deleteById(contactId);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/aggregate")
    public ApiResult<Map<String, Object>> aggregate(@PathVariable Long id) {
        Customer customer = check(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customer", customer);
        result.put("contacts", contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, id).orderByDesc(Contact::getPrimaryFlag)));
        List<Demand> demands = demandMapper.selectList(new LambdaQueryWrapper<Demand>()
                .eq(Demand::getCustomerId, id).orderByDesc(Demand::getId));
        result.put("demands", demands);
        result.put("consultations", consultationMapper.selectList(new LambdaQueryWrapper<Consultation>()
                .eq(Consultation::getCustomerId, id).orderByDesc(Consultation::getId)));
        List<Long> demandIds = demands.stream().map(Demand::getId).toList();
        result.put("solutions", demandIds.isEmpty() ? List.of() : solutionMapper.selectList(
                new LambdaQueryWrapper<Solution>().in(Solution::getDemandId, demandIds).orderByDesc(Solution::getId)));
        result.put("quotes", quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getCustomerId, id).orderByDesc(Quote::getId)));
        List<Contract> contracts = contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getCustomerId, id).orderByDesc(Contract::getId));
        result.put("contracts", contracts);
        List<Long> contractIds = contracts.stream().map(Contract::getId).toList();
        result.put("payments", contractIds.isEmpty() ? List.of() : paymentPlanMapper.selectList(
                new LambdaQueryWrapper<PaymentPlan>().in(PaymentPlan::getContractId, contractIds).orderByAsc(PaymentPlan::getId)));
        result.put("tickets", ticketMapper.selectList(new LambdaQueryWrapper<ServiceTicket>()
                .eq(ServiceTicket::getCustomerId, id).orderByDesc(ServiceTicket::getId)));
        result.put("equipment", equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getCustomerId, id).orderByDesc(Equipment::getId)));
        result.put("certificates", certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getCustomerId, id).orderByDesc(Certificate::getId)));
        result.put("referrals", referralMapper.selectList(new LambdaQueryWrapper<Referral>()
                .eq(Referral::getSourceCustomerId, id).orderByDesc(Referral::getId)));
        result.put("visits", visitMapper.selectList(new LambdaQueryWrapper<CustomerVisit>()
                .eq(CustomerVisit::getCustomerId, id).orderByDesc(CustomerVisit::getId)));
        BigDecimal dealAmount = contracts.stream().filter(c -> "PERFORMING".equals(c.getStatus()) || "DONE".equals(c.getStatus()))
                .map(c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("dealAmount", dealAmount);
        return ApiResult.ok(result);
    }

    @PostMapping("/import")
    public ApiResult<Map<String, Object>> importCustomers(@RequestParam("file") MultipartFile file) throws IOException {
        rateLimiter.check("customer-import:" + AuthContext.userId(), 5, 60);
        List<Map<String, String>> rows = excelService.read(file);
        int success = 0;
        List<String> errors = new ArrayList<>();
        var user = AuthContext.require();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String name = row.getOrDefault("企业名称", row.get("name"));
            if (name == null || name.isBlank()) {
                errors.add("第" + (i + 2) + "行：企业名称为空");
                continue;
            }
            Long count = customerMapper.selectCount(new LambdaQueryWrapper<Customer>().eq(Customer::getName, name));
            if (count > 0) {
                errors.add("第" + (i + 2) + "行：企业已存在 " + name);
                continue;
            }
            try {
                Customer customer = new Customer();
                customer.setTenantId(user.getTenantId());
                customer.setCustomerNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "CUSTOMER"));
                customer.setName(name);
                customer.setIndustry(row.getOrDefault("行业", ""));
                customer.setScale(row.getOrDefault("规模", ""));
                customer.setRegion(row.getOrDefault("地区", ""));
                customer.setAddress(row.getOrDefault("地址", ""));
                customer.setSource(row.getOrDefault("客户来源", "MANUAL"));
                customer.setCategory(row.getOrDefault("客户分类", ""));
                customer.setLevel(row.getOrDefault("客户级别", "B"));
                customer.setStatus("NORMAL");
                customer.setOwnerId(user.getId());
                customer.setRemark(row.getOrDefault("备注", ""));
                customerMapper.insert(customer);
                String contactName = row.getOrDefault("联系人", "");
                String phone = row.getOrDefault("联系电话", "");
                if (!contactName.isBlank() || !phone.isBlank()) {
                    Contact contact = new Contact();
                    contact.setTenantId(user.getTenantId());
                    contact.setCustomerId(customer.getId());
                    contact.setName(contactName.isBlank() ? name : contactName);
                    contact.setTitle("联系人");
                    contact.setPhoneEnc(phone.isBlank() ? "" : cryptoService.encrypt(phone));
                    contact.setPhoneMask(cryptoService.maskPhone(phone));
                    contact.setEmail(row.getOrDefault("邮箱", ""));
                    contact.setPrimaryFlag(1);
                    contactMapper.insert(contact);
                }
                success++;
            } catch (Exception e) {
                errors.add("第" + (i + 2) + "行：" + e.getMessage());
            }
        }
        auditService.log("客户档案", "批量导入", file.getOriginalFilename(), "成功" + success + "条，失败" + errors.size() + "条");
        return ApiResult.ok(Map.of("success", success, "failed", errors.size(), "errors", errors));
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(Customer::getId);
        List<Customer> customers = customerMapper.selectList(qw);
        List<List<Object>> rows = new ArrayList<>();
        for (Customer c : customers) {
            rows.add(Arrays.asList(c.getCustomerNo(), c.getName(), c.getIndustry(), c.getScale(), c.getRegion(),
                    c.getAddress(), c.getSource(), c.getCategory(), c.getLevel(), c.getStatus()));
        }
        excelService.export(response, "客户档案.xlsx",
                List.of("客户编号", "企业名称", "行业", "规模", "地区", "地址", "来源", "分类", "级别", "状态"), rows);
        auditService.log("客户档案", "导出", null, "共" + customers.size() + "条");
    }

    private Customer check(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            auditService.log("客户档案", "越权或不存在访问", String.valueOf(id), "记录不存在或越权访问被拦截");
            throw new BizException(404, "客户不存在或无权访问");
        }
        return customer;
    }

    private void encryptPhone(Contact contact) {
        String phone = contact.getPhoneEnc();
        if (phone != null && !phone.isBlank()) {
            contact.setPhoneEnc(cryptoService.encrypt(phone));
            contact.setPhoneMask(cryptoService.maskPhone(phone));
        }
    }
}
