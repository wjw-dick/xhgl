package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/public/share")
@RequiredArgsConstructor
public class PublicShareController {
    private final SolutionMapper solutionMapper;
    private final QuoteMapper quoteMapper;
    private final NegotiationMapper negotiationMapper;
    private final DemandMapper demandMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final CertificateMapper certificateMapper;
    private final TenantMapper tenantMapper;
    private final ShareLogMapper shareLogMapper;
    private final QuoteFlowService quoteFlowService;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;
    private final AuditService auditService;

    @GetMapping("/solution")
    public ApiResult<Map<String, Object>> solution(@RequestParam String token,
                                                   @RequestParam(defaultValue = "link") String source,
                                                   HttpServletRequest request) {
        rateLimiter.check("share-solution:" + request.getRemoteAddr(), 30, 60);
        Solution solution = solutionMapper.selectOne(new LambdaQueryWrapper<Solution>()
                .eq(Solution::getShareToken, token).last("limit 1"));
        if (solution == null) throw new BizException(404, "分享链接无效或已失效");
        Demand demand = solution.getDemandId() == null ? null : demandMapper.selectById(solution.getDemandId());
        Tenant tenant = tenantMapper.selectById(solution.getTenantId());
        solution.setViewCount((solution.getViewCount() == null ? 0 : solution.getViewCount()) + 1);
        solutionMapper.updateById(solution);
        logShare(solution.getTenantId(), "SOLUTION", solution.getId(), solution.getSolutionNo(), source, request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("solution", safeSolution(solution));
        result.put("demand", demand == null ? null : safeDemand(demand));
        result.put("company", tenant == null ? null : Map.of("name", tenant.getName(),
                "contactPhone", tenant.getContactPhone() == null ? "" : tenant.getContactPhone()));
        Product product = solution.getProductId() == null ? null : productMapper.selectById(solution.getProductId());
        result.put("product", product);
        if (product != null) {
            result.put("certificates", certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                    .eq(Certificate::getProductId, product.getId())).stream()
                    .map(c -> Map.of("certType", c.getCertType(), "validTo", String.valueOf(c.getValidTo()),
                            "status", effectiveStatus(c))).toList());
        }
        return ApiResult.ok(result);
    }

    @PostMapping("/solution/confirm")
    public ApiResult<Map<String, Object>> confirmSolution(@RequestParam String token,
                                                          HttpServletRequest request) {
        rateLimiter.check("confirm-solution:" + request.getRemoteAddr(), 10, 60);
        Solution solution = solutionMapper.selectOne(new LambdaQueryWrapper<Solution>()
                .eq(Solution::getShareToken, token).last("limit 1"));
        if (solution == null) throw new BizException(404, "分享链接无效");
        if ("CONFIRMED".equals(solution.getStatus())) {
            return ApiResult.ok(Map.of("confirmed", true, "message", "方案已确认"));
        }
        solution.setStatus("CONFIRMED");
        solution.setConfirmTime(java.time.LocalDateTime.now());
        solutionMapper.updateById(solution);
        Demand demand = solution.getDemandId() == null ? null : demandMapper.selectById(solution.getDemandId());
        if (demand != null && Set.of("NEW", "CONSULTING", "INTRODUCING").contains(demand.getStatus())) {
            demand.setStatus("TALKING");
            demandMapper.updateById(demand);
            notificationService.notifyTenant(solution.getTenantId(), demand.getOwnerId(), "客户已确认方案",
                    "需求 " + demand.getDemandNo() + " 方案已确认，可进入报价谈价", "SOLUTION_CONFIRMED", "/quotes");
        }
        auditService.logForTenant(solution.getTenantId(), "产品方案", "客户确认", "客户",
                solution.getSolutionNo(), null);
        return ApiResult.ok(Map.of("confirmed", true, "message", "方案已确认，业务人员将与您联系报价"));
    }

    @GetMapping("/quote")
    public ApiResult<Map<String, Object>> quote(@RequestParam String token,
                                                @RequestParam(defaultValue = "link") String source,
                                                HttpServletRequest request) {
        rateLimiter.check("share-quote:" + request.getRemoteAddr(), 30, 60);
        Quote quote = quoteMapper.selectOne(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getShareToken, token).last("limit 1"));
        if (quote == null) throw new BizException(404, "报价链接无效或已失效");
        if (quote.getValidUntil() != null && quote.getValidUntil().isBefore(LocalDate.now())
                && !"EXPIRED".equals(quote.getStatus())) {
            quote.setStatus("EXPIRED");
            quoteMapper.updateById(quote);
        }
        logShare(quote.getTenantId(), "QUOTE", quote.getId(), quote.getQuoteNo(), source, request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("quote", safeQuote(quote));
        result.put("negotiations", negotiationMapper.selectList(new LambdaQueryWrapper<Negotiation>()
                .eq(Negotiation::getQuoteId, quote.getId()).orderByAsc(Negotiation::getRoundNo)));
        result.put("demand", quote.getDemandId() == null ? null
                : safeDemand(demandMapper.selectById(quote.getDemandId())));
        Tenant tenant = tenantMapper.selectById(quote.getTenantId());
        result.put("company", tenant == null ? null : Map.of("name", tenant.getName(),
                "contactPhone", tenant.getContactPhone() == null ? "" : tenant.getContactPhone()));
        result.put("canConfirm", Set.of("SENT", "BARGAINING", "APPROVED").contains(quote.getStatus())
                && (quote.getValidUntil() == null || !quote.getValidUntil().isBefore(LocalDate.now())));
        return ApiResult.ok(result);
    }

    @PostMapping("/quote/confirm")
    public ApiResult<Map<String, Object>> confirmQuote(@RequestParam String token,
                                                       HttpServletRequest request) {
        rateLimiter.check("confirm-quote:" + request.getRemoteAddr(), 10, 60);
        rateLimiter.once("quote-confirm:" + token, 600);
        Quote quote = quoteMapper.selectOne(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getShareToken, token).last("limit 1"));
        if (quote == null) throw new BizException(404, "报价链接无效");
        quoteFlowService.confirm(quote, true);
        auditService.logForTenant(quote.getTenantId(), "报价议价", "客户在线确认", "客户",
                quote.getQuoteNo(), null);
        return ApiResult.ok(Map.of("confirmed", true, "message", "报价已确认，合同草稿已生成"));
    }

    @PostMapping("/quote/bargain")
    public ApiResult<Negotiation> bargain(@RequestParam String token, @RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        rateLimiter.check("bargain:" + request.getRemoteAddr(), 10, 60);
        Quote quote = quoteMapper.selectOne(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getShareToken, token).last("limit 1"));
        if (quote == null) throw new BizException(404, "报价链接无效");
        if ("EXPIRED".equals(quote.getStatus())) throw new BizException("报价已过期，无法还价");
        if (body.get("amount") == null) throw new BizException("请输入还价金额");
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("还价金额必须大于0");
        int round = negotiationMapper.selectList(new LambdaQueryWrapper<Negotiation>()
                .eq(Negotiation::getQuoteId, quote.getId())).size() + 1;
        Negotiation negotiation = new Negotiation();
        negotiation.setTenantId(quote.getTenantId());
        negotiation.setQuoteId(quote.getId());
        negotiation.setRoundNo(round);
        negotiation.setParty("CUSTOMER");
        negotiation.setAmount(amount);
        negotiation.setRemark(String.valueOf(body.getOrDefault("remark", "客户在线还价")));
        negotiation.setOperator("客户");
        negotiationMapper.insert(negotiation);
        quote.setStatus("BARGAINING");
        quote.setNegotiationCount(round);
        quoteMapper.updateById(quote);
        notificationService.notifyTenant(quote.getTenantId(), quote.getOwnerId(), "客户还价提醒",
                "报价单 " + quote.getQuoteNo() + " 客户还价 " + amount + " 元", "QUOTE_BARGAIN", "/quotes");
        auditService.logForTenant(quote.getTenantId(), "报价议价", "客户还价", "客户",
                quote.getQuoteNo(), "金额 " + amount);
        return ApiResult.ok(negotiation);
    }

    @GetMapping("/demand")
    public ApiResult<Map<String, Object>> demand(@RequestParam String demandNo,
                                                 @RequestParam String token,
                                                 @RequestParam(defaultValue = "link") String source,
                                                 HttpServletRequest request) {
        rateLimiter.check("share-demand:" + request.getRemoteAddr(), 30, 60);
        Demand demand = demandMapper.selectOne(new LambdaQueryWrapper<Demand>()
                .eq(Demand::getDemandNo, demandNo).eq(Demand::getShareToken, token).last("limit 1"));
        if (demand == null) throw new BizException(404, "查询链接无效");
        logShare(demand.getTenantId(), "DEMAND", demand.getId(), demand.getDemandNo(), source, request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("demand", safeDemand(demand));
        result.put("solutions", solutionMapper.selectList(new LambdaQueryWrapper<Solution>()
                .eq(Solution::getDemandId, demand.getId()).ne(Solution::getStatus, "DRAFT")).stream()
                .map(this::safeSolution).toList());
        result.put("quotes", quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getDemandId, demand.getId()).in(Quote::getStatus, "SENT", "BARGAINING", "CONFIRMED", "EXPIRED"))
                .stream().map(this::safeQuote).toList());
        result.put("customerName", customerMapper.selectById(demand.getCustomerId()) == null ? null
                : customerMapper.selectById(demand.getCustomerId()).getName());
        return ApiResult.ok(result);
    }

    private void logShare(Long tenantId, String bizType, Long bizId, String bizNo, String source, HttpServletRequest request) {
        ShareLog log = new ShareLog();
        log.setTenantId(tenantId);
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setBizNo(bizNo);
        log.setSource(source);
        log.setIp(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        shareLogMapper.insert(log);
    }

    private Map<String, Object> safeQuote(Quote quote) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", quote.getId());
        map.put("quoteNo", quote.getQuoteNo());
        map.put("version", quote.getVersion());
        map.put("productName", quote.getProductName());
        map.put("unitPrice", quote.getUnitPrice());
        map.put("quantity", quote.getQuantity());
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

    private Map<String, Object> safeSolution(Solution solution) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", solution.getId());
        map.put("solutionNo", solution.getSolutionNo());
        map.put("name", solution.getName());
        map.put("version", solution.getVersion());
        map.put("productName", solution.getProductName());
        map.put("content", solution.getContent());
        map.put("attachments", solution.getAttachments());
        map.put("status", solution.getStatus());
        map.put("confirmTime", solution.getConfirmTime());
        return map;
    }

    private Map<String, Object> safeDemand(Demand demand) {
        if (demand == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("demandNo", demand.getDemandNo());
        map.put("status", demand.getStatus());
        map.put("boilerType", demand.getBoilerType());
        map.put("fuelType", demand.getFuelType());
        map.put("capacity", demand.getCapacity());
        map.put("pressureLevel", demand.getPressureLevel());
        map.put("usageDesc", demand.getUsageDesc());
        map.put("region", demand.getRegion());
        map.put("expectTime", demand.getExpectTime());
        return map;
    }

    private String effectiveStatus(Certificate cert) {
        if (cert.getValidTo() == null) return "UNKNOWN";
        if (cert.getValidTo().isBefore(LocalDate.now())) return "EXPIRED";
        return "VALID";
    }
}
