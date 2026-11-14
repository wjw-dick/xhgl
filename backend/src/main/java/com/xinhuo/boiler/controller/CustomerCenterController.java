package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerCenterController {
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final DemandMapper demandMapper;
    private final FollowRecordMapper followMapper;
    private final SolutionMapper solutionMapper;
    private final QuoteMapper quoteMapper;
    private final NegotiationMapper negotiationMapper;
    private final ContractMapper contractMapper;
    private final PaymentPlanMapper paymentMapper;
    private final InstallationMapper installationMapper;
    private final SalesOrderMapper orderMapper;
    private final ServiceTicketMapper ticketMapper;
    private final TicketRecordMapper ticketRecordMapper;
    private final EquipmentMapper equipmentMapper;
    private final MaintenancePlanMapper maintenanceMapper;
    private final SatisfactionMapper satisfactionMapper;
    private final NotificationMapper notificationMapper;
    private final TenantUserMapper tenantUserMapper;
    private final TenantMapper tenantMapper;
    private final IdGenerator idGenerator;
    private final QuoteFlowService quoteFlowService;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;
    private final AuditService auditService;

    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        Long customerId = customerId();
        Customer customer = customerMapper.selectById(customerId);
        List<Demand> demands = demandMapper.selectList(new LambdaQueryWrapper<Demand>()
                .eq(Demand::getCustomerId, customerId).orderByDesc(Demand::getId));
        List<Long> demandIds = demands.stream().map(Demand::getId).toList();
        List<Solution> solutions = demandIds.isEmpty() ? List.of() : solutionMapper.selectList(
                new LambdaQueryWrapper<Solution>().in(Solution::getDemandId, demandIds).ne(Solution::getStatus, "DRAFT"));
        List<Quote> quotes = quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getCustomerId, customerId).ne(Quote::getStatus, "DRAFT").orderByDesc(Quote::getId));
        List<Contract> contracts = contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getCustomerId, customerId).orderByDesc(Contract::getId));
        List<ServiceTicket> tickets = ticketMapper.selectList(new LambdaQueryWrapper<ServiceTicket>()
                .eq(ServiceTicket::getCustomerId, customerId).orderByDesc(ServiceTicket::getId));
        List<MaintenancePlan> maintenance = maintenanceMapper.selectList(new LambdaQueryWrapper<MaintenancePlan>()
                .eq(MaintenancePlan::getCustomerId, customerId).eq(MaintenancePlan::getStatus, "ACTIVE")
                .le(MaintenancePlan::getNextDate, LocalDate.now().plusDays(30)).orderByAsc(MaintenancePlan::getNextDate));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customer", customer);
        result.put("contacts", contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId).orderByDesc(Contact::getPrimaryFlag)));
        result.put("demandCount", demands.size());
        result.put("solutionCount", solutions.size());
        result.put("quoteCount", quotes.size());
        result.put("contractCount", contracts.size());
        result.put("ticketCount", tickets.size());
        result.put("maintenanceWarnings", maintenance);
        result.put("recentDemands", demands.stream().limit(3).toList());
        result.put("recentQuotes", quotes.stream().limit(3).map(this::safeQuote).toList());
        result.put("recentTickets", tickets.stream().limit(3).toList());
        result.put("unreadCount", notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getCustomerId, customerId).eq(Notification::getReadFlag, 0)));
        return ApiResult.ok(result);
    }

    @GetMapping("/demands")
    public ApiResult<List<Demand>> demands() {
        return ApiResult.ok(demandMapper.selectList(new LambdaQueryWrapper<Demand>()
                .eq(Demand::getCustomerId, customerId()).orderByDesc(Demand::getId)));
    }

    @GetMapping("/demands/{id}")
    public ApiResult<Map<String, Object>> demandDetail(@PathVariable Long id) {
        Demand demand = demand(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("demand", demand);
        result.put("followRecords", followMapper.selectList(new LambdaQueryWrapper<FollowRecord>()
                .eq(FollowRecord::getDemandId, id).orderByAsc(FollowRecord::getFollowTime)));
        result.put("solutions", solutionMapper.selectList(new LambdaQueryWrapper<Solution>()
                .eq(Solution::getDemandId, id).ne(Solution::getStatus, "DRAFT")));
        result.put("quotes", quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getDemandId, id).ne(Quote::getStatus, "DRAFT")));
        return ApiResult.ok(result);
    }

    @GetMapping("/solutions")
    public ApiResult<List<Solution>> solutions() {
        List<Long> ids = myDemandIds();
        if (ids.isEmpty()) return ApiResult.ok(List.of());
        return ApiResult.ok(solutionMapper.selectList(new LambdaQueryWrapper<Solution>()
                .in(Solution::getDemandId, ids).ne(Solution::getStatus, "DRAFT").orderByDesc(Solution::getId)));
    }

    @GetMapping("/solutions/{id}")
    public ApiResult<Solution> solution(@PathVariable Long id) {
        Solution solution = solutionMapper.selectById(id);
        if (solution == null) throw new BizException(404, "方案不存在");
        if (!myDemandIds().contains(solution.getDemandId())) throw new BizException(403, "无权访问该方案");
        return ApiResult.ok(solution);
    }

    @PostMapping("/solutions/{id}/confirm")
    public ApiResult<Map<String, Object>> confirmSolution(@PathVariable Long id) {
        Solution solution = solutionMapper.selectById(id);
        if (solution == null) throw new BizException(404, "方案不存在");
        if (!myDemandIds().contains(solution.getDemandId())) throw new BizException(403, "无权访问该方案");
        solution.setStatus("CONFIRMED");
        solution.setConfirmTime(LocalDateTime.now());
        solutionMapper.updateById(solution);
        Demand demand = demandMapper.selectById(solution.getDemandId());
        if (demand != null && Set.of("NEW", "CONSULTING", "INTRODUCING").contains(demand.getStatus())) {
            demand.setStatus("TALKING");
            demandMapper.updateById(demand);
        }
        auditService.log("客户中心", "确认方案", solution.getSolutionNo(), null);
        return ApiResult.ok(Map.of("confirmed", true));
    }

    @GetMapping("/quotes")
    public ApiResult<List<Map<String, Object>>> quotes() {
        return ApiResult.ok(quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getCustomerId, customerId()).ne(Quote::getStatus, "DRAFT").orderByDesc(Quote::getId))
                .stream().map(this::safeQuote).toList());
    }

    @GetMapping("/quotes/{id}")
    public ApiResult<Map<String, Object>> quoteDetail(@PathVariable Long id) {
        Quote quote = quote(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("quote", safeQuote(quote));
        result.put("negotiations", negotiationMapper.selectList(new LambdaQueryWrapper<Negotiation>()
                .eq(Negotiation::getQuoteId, id).orderByAsc(Negotiation::getRoundNo)));
        result.put("contracts", contractMapper.selectList(new LambdaQueryWrapper<Contract>().eq(Contract::getQuoteId, id)));
        return ApiResult.ok(result);
    }

    private Map<String, Object> safeQuote(Quote quote) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", quote.getId());
        map.put("quoteNo", quote.getQuoteNo());
        map.put("version", quote.getVersion());
        map.put("productName", quote.getProductName());
        map.put("unitPrice", quote.getUnitPrice());
        map.put("quantity", quote.getQuantity());
        map.put("taxRate", quote.getTaxRate());
        map.put("amount", quote.getAmount());
        map.put("freight", quote.getFreight());
        map.put("installFee", quote.getInstallFee());
        map.put("totalAmount", quote.getTotalAmount());
        map.put("paymentTerms", quote.getPaymentTerms());
        map.put("warranty", quote.getWarranty());
        map.put("validUntil", quote.getValidUntil());
        map.put("status", quote.getStatus());
        map.put("customerName", quote.getCustomerName());
        map.put("confirmTime", quote.getConfirmTime());
        return map;
    }

    @PostMapping("/quotes/{id}/confirm")
    public ApiResult<Map<String, Object>> confirmQuote(@PathVariable Long id,
                                                       HttpServletRequest request) {
        rateLimiter.check("customer-quote-confirm:" + AuthContext.userId(), 10, 60);
        Quote quote = quote(id);
        quoteFlowService.confirm(quote, true);
        auditService.log("客户中心", "确认报价", quote.getQuoteNo(), null);
        return ApiResult.ok(Map.of("confirmed", true, "message", "报价已确认，合同草稿已生成"));
    }

    @PostMapping("/quotes/{id}/bargain")
    public ApiResult<Negotiation> bargain(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        rateLimiter.check("customer-bargain:" + AuthContext.userId(), 10, 60);
        Quote quote = quote(id);
        if ("EXPIRED".equals(quote.getStatus())) throw new BizException("报价已过期");
        if (body.get("amount") == null) throw new BizException("请输入还价金额");
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        int round = negotiationMapper.selectList(new LambdaQueryWrapper<Negotiation>()
                .eq(Negotiation::getQuoteId, id)).size() + 1;
        Negotiation negotiation = new Negotiation();
        negotiation.setTenantId(quote.getTenantId());
        negotiation.setQuoteId(id);
        negotiation.setRoundNo(round);
        negotiation.setParty("CUSTOMER");
        negotiation.setAmount(amount);
        negotiation.setRemark(String.valueOf(body.getOrDefault("remark", "客户还价")));
        negotiation.setOperator(AuthContext.require().getName());
        negotiationMapper.insert(negotiation);
        quote.setStatus("BARGAINING");
        quote.setNegotiationCount(round);
        quoteMapper.updateById(quote);
        notificationService.notifyTenant(quote.getTenantId(), quote.getOwnerId(), "客户还价提醒",
                "报价单 " + quote.getQuoteNo() + " 客户还价 " + amount, "QUOTE_BARGAIN", "/quotes");
        auditService.log("客户中心", "还价", quote.getQuoteNo(), String.valueOf(amount));
        return ApiResult.ok(negotiation);
    }

    @GetMapping("/contracts")
    public ApiResult<List<Contract>> contracts() {
        return ApiResult.ok(contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getCustomerId, customerId()).orderByDesc(Contract::getId)));
    }

    @GetMapping("/contracts/{id}")
    public ApiResult<Map<String, Object>> contractDetail(@PathVariable Long id) {
        Contract contract = contract(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contract", contract);
        result.put("payments", paymentMapper.selectList(new LambdaQueryWrapper<PaymentPlan>()
                .eq(PaymentPlan::getContractId, id).orderByAsc(PaymentPlan::getId)));
        result.put("installations", installationMapper.selectList(new LambdaQueryWrapper<Installation>()
                .eq(Installation::getContractId, id).orderByAsc(Installation::getId)));
        result.put("order", orderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getContractId, id).last("limit 1")));
        result.put("equipment", equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getContractId, id)));
        return ApiResult.ok(result);
    }

    @PostMapping("/contracts/{id}/sign")
    public ApiResult<Contract> signContract(@PathVariable Long id) {
        Contract contract = contract(id);
        if (!"APPROVED".equals(contract.getAuditStatus())) throw new BizException("合同尚未审批通过，暂不可签署");
        if ("SIGNED".equals(contract.getSignStatus())) return ApiResult.ok(contract);
        contract.setSignStatus("SIGNED");
        contract.setSignType("CUSTOMER_ONLINE");
        contract.setSignTime(LocalDateTime.now());
        contract.setStatus("PERFORMING");
        contractMapper.updateById(contract);
        if (orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getContractId, id)) == 0) {
            Tenant tenant = tenantMapper.selectById(AuthContext.tenantId());
            SalesOrder order = new SalesOrder();
            order.setTenantId(contract.getTenantId());
            order.setOrderNo(idGenerator.next(contract.getTenantId(), tenant == null ? "T" : tenant.getCode(), "ORDER"));
            order.setContractId(id);
            order.setProductionProgress("0%");
            order.setDeliveryStatus("PENDING");
            order.setExpectedDelivery(LocalDate.now().plusDays(45));
            orderMapper.insert(order);
        }
        if (paymentMapper.selectCount(new LambdaQueryWrapper<PaymentPlan>().eq(PaymentPlan::getContractId, id)) == 0) {
            BigDecimal total = contract.getAmount() == null ? BigDecimal.ZERO : contract.getAmount();
            addPlan(contract, "定金", total.multiply(new BigDecimal("0.3")), LocalDate.now());
            addPlan(contract, "发货款", total.multiply(new BigDecimal("0.4")), LocalDate.now().plusDays(30));
            addPlan(contract, "验收尾款", total.multiply(new BigDecimal("0.3")), LocalDate.now().plusDays(60));
        }
        if (installationMapper.selectCount(new LambdaQueryWrapper<Installation>().eq(Installation::getContractId, id)) == 0) {
            for (String stage : List.of("SHIPPED", "ARRIVED", "INSTALL", "ACCEPTANCE")) {
                Installation installation = new Installation();
                installation.setTenantId(contract.getTenantId());
                installation.setContractId(id);
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
        notificationService.notifyTenant(contract.getTenantId(), contract.getOwnerId(), "客户已在线签署合同",
                "合同 " + contract.getContractNo() + " 已签署", "CONTRACT_SIGNED", "/contracts");
        auditService.log("客户中心", "合同签署", contract.getContractNo(), "CUSTOMER_ONLINE");
        return ApiResult.ok(contract);
    }

    @GetMapping("/tickets")
    public ApiResult<List<ServiceTicket>> tickets() {
        return ApiResult.ok(ticketMapper.selectList(new LambdaQueryWrapper<ServiceTicket>()
                .eq(ServiceTicket::getCustomerId, customerId()).orderByDesc(ServiceTicket::getId)));
    }

    @GetMapping("/tickets/{id}")
    public ApiResult<Map<String, Object>> ticket(@PathVariable Long id) {
        ServiceTicket ticket = myTicket(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticket", ticket);
        result.put("records", ticketRecordMapper.selectList(new LambdaQueryWrapper<TicketRecord>()
                .eq(TicketRecord::getTicketId, id).orderByAsc(TicketRecord::getCreatedAt)));
        result.put("maintenance", maintenanceMapper.selectList(new LambdaQueryWrapper<MaintenancePlan>()
                .eq(MaintenancePlan::getCustomerId, customerId()).eq(MaintenancePlan::getEquipmentId, ticket.getEquipmentId())));
        return ApiResult.ok(result);
    }

    @PostMapping("/tickets")
    public ApiResult<ServiceTicket> createTicket(@RequestBody ServiceTicket body, HttpServletRequest request) {
        rateLimiter.check("customer-ticket:" + AuthContext.userId(), 5, 60);
        Long customerId = customerId();
        Customer customer = customerMapper.selectById(customerId);
        if (body.getEquipmentId() != null) {
            Equipment equipment = equipmentMapper.selectById(body.getEquipmentId());
            if (equipment == null || !equipment.getCustomerId().equals(customerId)) throw new BizException(403, "设备不属于当前客户");
        }
        for (String active : List.of("ACCEPTED", "DISPATCHED", "PROCESSING", "WAIT_REVIEW")) {
            ServiceTicket existing = ticketMapper.selectOne(new LambdaQueryWrapper<ServiceTicket>()
                    .eq(ServiceTicket::getCustomerId, customerId)
                    .eq(body.getEquipmentId() != null, ServiceTicket::getEquipmentId, body.getEquipmentId())
                    .eq(ServiceTicket::getType, body.getType()).eq(ServiceTicket::getStatus, active)
                    .ge(ServiceTicket::getCreatedAt, LocalDateTime.now().minusHours(24)).last("limit 1"));
            if (existing != null) throw new BizException(409, "存在24小时内同类未完结工单 " + existing.getTicketNo());
        }
        var user = AuthContext.require();
        ServiceTicket ticket = new ServiceTicket();
        ticket.setTenantId(user.getTenantId());
        ticket.setTicketNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "TICKET"));
        ticket.setCustomerId(customerId);
        ticket.setCustomerName(customer.getName());
        ticket.setEquipmentId(body.getEquipmentId());
        if (body.getEquipmentId() != null) {
            Equipment equipment = equipmentMapper.selectById(body.getEquipmentId());
            ticket.setEquipmentNo(equipment.getEquipmentNo());
        }
        ticket.setType(body.getType() == null ? "REPAIR" : body.getType());
        ticket.setDescription(body.getDescription());
        if (body.getPhotos() != null && !body.getPhotos().isBlank()) {
            List<String> photos = Arrays.stream(body.getPhotos().split(",")).map(String::trim)
                    .filter(url -> !url.isEmpty()).toList();
            if (photos.size() > 6) throw new BizException("最多上传 6 张报修照片");
            for (String url : photos) {
                if (!url.startsWith("/uploads/") && !url.startsWith("http://") && !url.startsWith("https://")) {
                    throw new BizException("报修照片地址非法");
                }
            }
            ticket.setPhotos(String.join(",", photos));
        }
        ticket.setPriority(body.getPriority() == null ? "NORMAL" : body.getPriority());
        ticket.setStatus("ACCEPTED");
        ticket.setAcceptedTime(LocalDateTime.now());
        ticket.setOwnerId(customer.getOwnerId());
        ticket.setContactName(body.getContactName() == null ? customer.getName() : body.getContactName());
        ticket.setContactPhoneMask(body.getContactPhoneMask());
        ticket.setSource("MP");
        ticketMapper.insert(ticket);
        addTicketRecord(ticket, "客户", "受理", "客户在线报修，工单已受理");
        for (TenantUser manager : tenantUserMapper.selectList(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, user.getTenantId()).in(TenantUser::getRoleCode,
                        "ADMIN"))) {
            notificationService.notifyTenant(user.getTenantId(), manager.getId(), "新报修工单",
                    "客户 " + customer.getName() + " 提交报修：" + ticket.getDescription(), "TICKET_NOTICE", "/tickets");
        }
        auditService.log("客户中心", "在线报修", ticket.getTicketNo(), ticket.getDescription());
        return ApiResult.ok(ticket);
    }

    @PostMapping("/tickets/{id}/review")
    public ApiResult<ServiceTicket> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ServiceTicket ticket = myTicket(id);
        if (!"WAIT_REVIEW".equals(ticket.getStatus()) && !"COMPLETED".equals(ticket.getStatus())) {
            throw new BizException("工单尚未完工，暂不可评价");
        }
        int score = Integer.parseInt(String.valueOf(body.getOrDefault("score", 5)));
        String comment = String.valueOf(body.getOrDefault("comment", ""));
        ticket.setStatus("COMPLETED");
        ticket.setSatisfactionScore(score);
        ticket.setReview(comment);
        ticketMapper.updateById(ticket);
        Satisfaction satisfaction = new Satisfaction();
        satisfaction.setTenantId(ticket.getTenantId());
        satisfaction.setTicketId(id);
        satisfaction.setCustomerId(customerId());
        satisfaction.setScore(score);
        satisfaction.setComment(comment);
        satisfaction.setSource("CUSTOMER");
        satisfactionMapper.insert(satisfaction);
        notificationService.notifyTenant(ticket.getTenantId(), ticket.getOwnerId(), "客户满意度评价",
                "工单 " + ticket.getTicketNo() + " " + score + "星：" + comment, "SATISFACTION", "/tickets");
        auditService.log("客户中心", "满意度评价", ticket.getTicketNo(), score + "星");
        return ApiResult.ok(ticket);
    }

    @GetMapping("/maintenance")
    public ApiResult<List<MaintenancePlan>> maintenance() {
        return ApiResult.ok(maintenanceMapper.selectList(new LambdaQueryWrapper<MaintenancePlan>()
                .eq(MaintenancePlan::getCustomerId, customerId()).eq(MaintenancePlan::getStatus, "ACTIVE")
                .orderByAsc(MaintenancePlan::getNextDate)));
    }

    @GetMapping("/equipment")
    public ApiResult<List<Equipment>> equipment() {
        return ApiResult.ok(equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getCustomerId, customerId()).orderByDesc(Equipment::getId)));
    }

    @GetMapping("/notifications")
    public ApiResult<PageResult<Notification>> notifications(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "10") long size) {
        Page<Notification> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        return ApiResult.ok(PageResult.of(notificationMapper.selectPage(p, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getCustomerId, customerId()).eq(Notification::getUserType, "CUSTOMER")
                .orderByDesc(Notification::getId))));
    }

    @PostMapping("/notifications/{id}/read")
    public ApiResult<Void> readNotification(@PathVariable Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !Objects.equals(notification.getCustomerId(), customerId())) {
            throw new BizException(404, "消息不存在");
        }
        notification.setReadFlag(1);
        notification.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(notification);
        return ApiResult.ok();
    }

    @GetMapping("/profile")
    public ApiResult<Map<String, Object>> profile() {
        Customer customer = customerMapper.selectById(customerId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customer", customer);
        result.put("contacts", contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId())));
        result.put("user", AuthContext.require());
        return ApiResult.ok(result);
    }

    @PutMapping("/profile")
    public ApiResult<Map<String, Object>> updateProfile(@RequestBody Map<String, Object> body) {
        rateLimiter.check("customer-profile:" + AuthContext.userId(), 10, 60);
        Customer customer = customerMapper.selectById(customerId());
        if (customer == null) throw new BizException(404, "客户档案不存在");
        String industry = profileValue(body.get("industry"), "所属行业");
        String region = profileValue(body.get("region"), "地区");
        customer.setIndustry(industry);
        customer.setRegion(region);
        customerMapper.updateById(customer);
        auditService.log("客户中心", "更新企业档案", customer.getCustomerNo(),
                "行业：" + (industry == null ? "未填写" : industry) + "，地区：" + (region == null ? "未填写" : region));
        return profile();
    }

    private String profileValue(Object value, String label) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        if (text.length() > 64) throw new BizException(label + "不能超过 64 个字符");
        return text.isBlank() ? null : text;
    }

    private Long customerId() {
        var user = AuthContext.require();
        if (!user.isCustomer() || user.getCustomerId() == null) throw new BizException(403, "仅用户可访问");
        return user.getCustomerId();
    }

    private Demand demand(Long id) {
        Demand demand = demandMapper.selectById(id);
        if (demand == null || !Objects.equals(demand.getCustomerId(), customerId())) throw new BizException(403, "无权访问该需求");
        return demand;
    }

    private Quote quote(Long id) {
        Quote quote = quoteMapper.selectById(id);
        if (quote == null || !Objects.equals(quote.getCustomerId(), customerId())) throw new BizException(403, "无权访问该报价");
        return quote;
    }

    private Contract contract(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null || !Objects.equals(contract.getCustomerId(), customerId())) throw new BizException(403, "无权访问该合同");
        return contract;
    }

    private ServiceTicket myTicket(Long id) {
        ServiceTicket ticket = ticketMapper.selectById(id);
        if (ticket == null || !Objects.equals(ticket.getCustomerId(), customerId())) throw new BizException(403, "无权访问该工单");
        return ticket;
    }

    private List<Long> myDemandIds() {
        return demandMapper.selectList(new LambdaQueryWrapper<Demand>()
                .eq(Demand::getCustomerId, customerId())).stream().map(Demand::getId).toList();
    }

    private void addTicketRecord(ServiceTicket ticket, String operator, String action, String content) {
        TicketRecord record = new TicketRecord();
        record.setTenantId(ticket.getTenantId());
        record.setTicketId(ticket.getId());
        record.setOperator(operator);
        record.setAction(action);
        record.setContent(content);
        ticketRecordMapper.insert(record);
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
}
