package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicDemandController {
    private final TenantMapper tenantMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final DemandMapper demandMapper;
    private final FollowRecordMapper followMapper;
    private final ConsultationMapper consultationMapper;
    private final FaqMapper faqMapper;
    private final SolutionMapper solutionMapper;
    private final QuoteMapper quoteMapper;
    private final TenantUserMapper tenantUserMapper;
    private final IdGenerator idGenerator;
    private final CryptoService cryptoService;
    private final RateLimiter rateLimiter;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Data
    public static class PublicDemandRequest {
        private String customerName;
        private String contactName;
        private String phone;
        private String boilerType;
        private String fuelType;
        private String capacity;
        private String pressureLevel;
        private String usageDesc;
        private BigDecimal budgetMin;
        private BigDecimal budgetMax;
        private String region;
        private LocalDate expectTime;
        private String description;
        private String source;
        private String language;
        private boolean force;
    }

    @Data
    public static class ConsultRequest {
        private String customerName;
        private String contactName;
        private String phone;
        private String demandNo;
        private String question;
        private String source;
    }

    @PostMapping("/demands")
    public ApiResult<Map<String, Object>> submit(@RequestBody PublicDemandRequest request,
                                                 HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        rateLimiter.check("public-demand:" + ip, 5, 60);
        String idem = http.getHeader("Idempotency-Key");
        if (idem != null && !idem.isBlank()) rateLimiter.once("idem-demand:" + idem, 600);
        Tenant tenant = company();
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) throw new BizException("企业名称不能为空");
        if (request.getPhone() == null || request.getPhone().isBlank()) throw new BizException("联系电话不能为空");
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantId, tenant.getId()).eq(Customer::getName, request.getCustomerName().trim())
                .last("limit 1"));
        if (customer != null) {
            Demand duplicate = demandMapper.selectOne(new LambdaQueryWrapper<Demand>()
                    .eq(Demand::getTenantId, tenant.getId()).eq(Demand::getCustomerId, customer.getId())
                    .notIn(Demand::getStatus, "LOST", "DEAL")
                    .ge(Demand::getCreatedAt, LocalDateTime.now().minusDays(30))
                    .orderByDesc(Demand::getId).last("limit 1"));
            if (duplicate != null && !request.isForce()) {
                throw new BizException(409, "检测到您近期已提交相似需求 " + duplicate.getDemandNo()
                        + "，请勿重复提交；如需求有变化请确认后继续提交");
            }
        }
        if (customer == null) {
            customer = new Customer();
            customer.setTenantId(tenant.getId());
            customer.setCustomerNo(idGenerator.next(tenant.getId(), tenant.getCode(), "CUSTOMER"));
            customer.setName(request.getCustomerName().trim());
            customer.setIndustry("");
            customer.setRegion(request.getRegion());
            customer.setAddress(request.getRegion());
            customer.setSource(request.getSource() == null ? "H5" : request.getSource());
            customer.setCategory("线上线索");
            customer.setLevel("B");
            customer.setStatus("NORMAL");
            customerMapper.insert(customer);
        }
        Contact contact = new Contact();
        contact.setTenantId(tenant.getId());
        contact.setCustomerId(customer.getId());
        contact.setName(request.getContactName() == null || request.getContactName().isBlank()
                ? request.getCustomerName() : request.getContactName());
        contact.setTitle("采购联系人");
        contact.setPhoneEnc(cryptoService.encrypt(request.getPhone()));
        contact.setPhoneMask(cryptoService.maskPhone(request.getPhone()));
        contact.setPrimaryFlag(1);
        contactMapper.insert(contact);

        Demand demand = new Demand();
        demand.setTenantId(tenant.getId());
        demand.setDemandNo(idGenerator.next(tenant.getId(), tenant.getCode(), "DEMAND"));
        demand.setCustomerId(customer.getId());
        demand.setContactId(contact.getId());
        demand.setContactName(contact.getName());
        demand.setContactPhoneMask(contact.getPhoneMask());
        demand.setBoilerType(request.getBoilerType());
        demand.setFuelType(request.getFuelType());
        demand.setCapacity(request.getCapacity());
        demand.setPressureLevel(request.getPressureLevel());
        demand.setUsageDesc(request.getUsageDesc());
        demand.setBudgetMin(request.getBudgetMin());
        demand.setBudgetMax(request.getBudgetMax());
        demand.setRegion(request.getRegion());
        demand.setExpectTime(request.getExpectTime());
        demand.setSource(request.getSource() == null ? "H5" : request.getSource());
        demand.setStatus("NEW");
        demand.setFollowNextTime(LocalDateTime.now().plusHours(2));
        demand.setDescription(request.getDescription());
        demand.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        demandMapper.insert(demand);
        for (TenantUser admin : tenantUserMapper.selectList(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tenant.getId()).eq(TenantUser::getRoleCode, "ADMIN"))) {
            notificationService.notifyTenant(tenant.getId(), admin.getId(), "新需求待分配",
                    "需求 " + demand.getDemandNo() + " 来自" + demand.getSource() + "，请及时分配负责人",
                    "DEMAND_CONFIRM", "/demands");
        }
        auditService.logForTenant(tenant.getId(), "需求线索", "公开提交", customer.getName(),
                demand.getDemandNo(), "来源=" + demand.getSource() + " IP=" + ip);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("demandNo", demand.getDemandNo());
        result.put("customerNo", customer.getCustomerNo());
        result.put("status", demand.getStatus());
        result.put("message", "需求已提交，业务人员将在2小时内联系您");
        result.put("trackToken", demand.getShareToken());
        return ApiResult.ok(result);
    }

    @PostMapping("/demands/duplicate")
    public ApiResult<Map<String, Object>> duplicate(@RequestBody Map<String, String> body) {
        Tenant tenant = company();
        String name = body.get("customerName");
        if (name == null || name.isBlank()) return ApiResult.ok(Map.of("duplicate", false));
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantId, tenant.getId()).eq(Customer::getName, name).last("limit 1"));
        if (customer == null) return ApiResult.ok(Map.of("duplicate", false));
        Demand existing = demandMapper.selectOne(new LambdaQueryWrapper<Demand>()
                .eq(Demand::getTenantId, tenant.getId()).eq(Demand::getCustomerId, customer.getId())
                .notIn(Demand::getStatus, "LOST", "DEAL")
                .ge(Demand::getCreatedAt, LocalDateTime.now().minusDays(30))
                .orderByDesc(Demand::getId).last("limit 1"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("duplicate", existing != null);
        if (existing != null) {
            result.put("demandNo", existing.getDemandNo());
            result.put("status", existing.getStatus());
        }
        return ApiResult.ok(result);
    }

    @GetMapping("/demands/track")
    public ApiResult<Map<String, Object>> track(@RequestParam String demandNo,
                                                @RequestParam(required = false) String phone,
                                                @RequestParam(required = false) String token,
                                                HttpServletRequest request) {
        rateLimiter.check("demand-track:" + request.getRemoteAddr(), 30, 60);
        Demand demand = demandMapper.selectOne(new LambdaQueryWrapper<Demand>()
                .eq(Demand::getDemandNo, demandNo).last("limit 1"));
        if (demand == null) throw new BizException(404, "需求不存在");
        if (token == null || !token.equals(demand.getShareToken())) {
            if (phone == null || !cryptoService.maskPhone(phone).equals(demand.getContactPhoneMask())) {
                throw new BizException(403, "查询凭证不正确");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("demandNo", demand.getDemandNo());
        result.put("status", demand.getStatus());
        result.put("boilerType", demand.getBoilerType());
        result.put("fuelType", demand.getFuelType());
        result.put("capacity", demand.getCapacity());
        result.put("createdAt", demand.getCreatedAt());
        result.put("region", demand.getRegion());
        result.put("solutions", solutionMapper.selectList(new LambdaQueryWrapper<Solution>()
                .eq(Solution::getDemandId, demand.getId()).ne(Solution::getStatus, "DRAFT")));
        result.put("quotes", quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getDemandId, demand.getId()).eq(Quote::getStatus, "CONFIRMED")));
        result.put("timeline", followMapper.selectList(new LambdaQueryWrapper<FollowRecord>()
                .eq(FollowRecord::getDemandId, demand.getId()).eq(FollowRecord::getMethod, "SYSTEM")
                .orderByAsc(FollowRecord::getFollowTime)));
        return ApiResult.ok(result);
    }

    @PostMapping("/consultations")
    public ApiResult<Map<String, Object>> consult(@RequestBody ConsultRequest request,
                                                  HttpServletRequest http) {
        rateLimiter.check("public-consult:" + http.getRemoteAddr(), 10, 60);
        Tenant tenant = company();
        if (request.getQuestion() == null || request.getQuestion().isBlank()) throw new BizException("咨询内容不能为空");
        Customer customer = null;
        if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getTenantId, tenant.getId()).eq(Customer::getName, request.getCustomerName())
                    .last("limit 1"));
            if (customer == null) {
                customer = new Customer();
                customer.setTenantId(tenant.getId());
                customer.setCustomerNo(idGenerator.next(tenant.getId(), tenant.getCode(), "CUSTOMER"));
                customer.setName(request.getCustomerName());
                customer.setSource(request.getSource() == null ? "H5" : request.getSource());
                customer.setCategory("在线咨询");
                customer.setStatus("NORMAL");
                customerMapper.insert(customer);
            }
            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                Contact contact = new Contact();
                contact.setTenantId(tenant.getId());
                contact.setCustomerId(customer.getId());
                contact.setName(request.getContactName() == null ? request.getCustomerName() : request.getContactName());
                contact.setPhoneEnc(cryptoService.encrypt(request.getPhone()));
                contact.setPhoneMask(cryptoService.maskPhone(request.getPhone()));
                contact.setPrimaryFlag(1);
                contactMapper.insert(contact);
            }
        }
        List<Faq> faqs = faqMapper.selectList(new LambdaQueryWrapper<Faq>()
                .eq(Faq::getTenantId, tenant.getId()).eq(Faq::getStatus, "NORMAL"));
        Faq matched = null;
        for (Faq faq : faqs) {
            String q = request.getQuestion();
            if ((faq.getQuestion() != null && faq.getQuestion().contains(q))
                    || (faq.getKeywords() != null && Arrays.stream(faq.getKeywords().split("[,，]"))
                    .anyMatch(k -> !k.isBlank() && q.contains(k.trim())))) {
                matched = faq;
                break;
            }
        }
        Consultation consultation = new Consultation();
        consultation.setTenantId(tenant.getId());
        consultation.setCustomerId(customer == null ? null : customer.getId());
        if (request.getDemandNo() != null && !request.getDemandNo().isBlank()) {
            Demand demand = demandMapper.selectOne(new LambdaQueryWrapper<Demand>()
                    .eq(Demand::getTenantId, tenant.getId()).eq(Demand::getDemandNo, request.getDemandNo()).last("limit 1"));
            if (demand != null) consultation.setDemandId(demand.getId());
        }
        consultation.setChannel("ONLINE");
        consultation.setQuestion(request.getQuestion());
        consultation.setCustomerName(request.getCustomerName());
        consultation.setContactPhoneMask(request.getPhone() == null ? "" : cryptoService.maskPhone(request.getPhone()));
        if (matched != null) {
            consultation.setAnswer(matched.getAnswer());
            consultation.setFaqId(matched.getId());
            consultation.setStatus("DONE");
        } else {
            consultation.setStatus("WAITING");
        }
        consultationMapper.insert(consultation);
        for (TenantUser admin : tenantUserMapper.selectList(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tenant.getId()).in(TenantUser::getRoleCode,
                        "ADMIN"))) {
            notificationService.notifyTenant(tenant.getId(), admin.getId(), "新在线咨询",
                    "客户 " + request.getCustomerName() + " 咨询：" + request.getQuestion(), "CONSULTATION", "/consultations");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consultationId", consultation.getId());
        result.put("autoAnswered", matched != null);
        result.put("answer", matched == null ? "已转人工，顾问会尽快回复您。" : matched.getAnswer());
        result.put("faqs", faqs.stream().limit(4).toList());
        return ApiResult.ok(result);
    }

    @GetMapping("/faqs")
    public ApiResult<List<Faq>> faqs() {
        Tenant tenant = company();
        return ApiResult.ok(faqMapper.selectList(new LambdaQueryWrapper<Faq>()
                .eq(Faq::getTenantId, tenant.getId()).eq(Faq::getStatus, "NORMAL").orderByAsc(Faq::getId)
                .last("limit 20")));
    }

    private Tenant company() {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, "HLJ").last("limit 1"));
        if (tenant == null) throw new BizException(404, "企业信息不存在");
        if ("DISABLED".equals(tenant.getStatus()) || "EXPIRED".equals(tenant.getStatus())) throw new BizException(403, "系统已停用");
        return tenant;
    }
}
