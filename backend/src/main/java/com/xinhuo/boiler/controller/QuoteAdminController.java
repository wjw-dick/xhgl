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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin/quotes")
@RequiredArgsConstructor
public class QuoteAdminController {
    private final QuoteMapper quoteMapper;
    private final NegotiationMapper negotiationMapper;
    private final DemandMapper demandMapper;
    private final CustomerMapper customerMapper;
    private final ContractMapper contractMapper;
    private final SolutionMapper solutionMapper;
    private final ProductMapper productMapper;
    private final IdGenerator idGenerator;
    private final QuoteFlowService quoteFlowService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ApiResult<PageResult<Quote>> list(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long customerId,
                                             @RequestParam(required = false) Long demandId) {
        var user = AuthContext.require();
        Page<Quote> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Quote> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) qw.and(w -> w.like(Quote::getQuoteNo, keyword)
                .or().like(Quote::getCustomerName, keyword).or().like(Quote::getProductName, keyword));
        if (status != null && !status.isBlank()) qw.eq(Quote::getStatus, status);
        if (customerId != null) qw.eq(Quote::getCustomerId, customerId);
        if (demandId != null) qw.eq(Quote::getDemandId, demandId);
        qw.orderByDesc(Quote::getId);
        return ApiResult.ok(PageResult.of(quoteMapper.selectPage(p, qw)));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Quote quote = find(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("quote", quote);
        result.put("negotiations", negotiationMapper.selectList(new LambdaQueryWrapper<Negotiation>()
                .eq(Negotiation::getQuoteId, id).orderByAsc(Negotiation::getRoundNo)));
        result.put("customer", quote.getCustomerId() == null ? null : customerMapper.selectById(quote.getCustomerId()));
        result.put("demand", quote.getDemandId() == null ? null : demandMapper.selectById(quote.getDemandId()));
        result.put("solution", quote.getSolutionId() == null ? null : solutionMapper.selectById(quote.getSolutionId()));
        result.put("contracts", contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getQuoteId, id)));
        List<Quote> versions = quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getDemandId, quote.getDemandId()).orderByAsc(Quote::getVersion));
        result.put("versions", versions);
        return ApiResult.ok(result);
    }

    @PostMapping
    public ApiResult<Quote> create(@RequestBody Quote quote) {
        var user = AuthContext.require();
        quote.setId(null);
        quote.setTenantId(user.getTenantId());
        quote.setQuoteNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "QUOTE"));
        long existingVersions = quote.getDemandId() == null ? 0 : quoteMapper.selectCount(
                new LambdaQueryWrapper<Quote>().eq(Quote::getDemandId, quote.getDemandId()));
        quote.setVersion((int) existingVersions + 1);
        quote.setOwnerId(user.getId());
        quote.setCustomerName(resolveCustomer(quote));
        quote.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        quote.setNegotiationCount(0);
        if (quote.getValidUntil() == null) quote.setValidUntil(LocalDate.now().plusDays(30));
        quoteFlowService.recalculate(quote);
        applyApproval(quote);
        quoteMapper.insert(quote);
        auditService.log("报价议价", "新增报价", quote.getQuoteNo(), "总额 " + quote.getTotalAmount());
        return ApiResult.ok(quote);
    }

    @PutMapping("/{id}")
    public ApiResult<Quote> update(@PathVariable Long id, @RequestBody Quote quote) {
        Quote existing = find(id);
        if (!Set.of("DRAFT", "APPROVAL", "REJECTED").contains(existing.getStatus())) {
            throw new BizException("已发送/已确认/已过期的报价不可编辑，请创建新版本");
        }
        quote.setId(id);
        quote.setTenantId(existing.getTenantId());
        quote.setQuoteNo(existing.getQuoteNo());
        quote.setVersion(existing.getVersion());
        quote.setOwnerId(existing.getOwnerId());
        quote.setShareToken(existing.getShareToken());
        quote.setCustomerName(existing.getCustomerName());
        quote.setStatus("DRAFT");
        quoteFlowService.recalculate(quote);
        applyApproval(quote);
        quoteMapper.updateById(quote);
        auditService.log("报价议价", "修改报价", existing.getQuoteNo(), "总额 " + quote.getTotalAmount());
        return ApiResult.ok(quoteMapper.selectById(id));
    }

    @PostMapping("/{id}/new-version")
    public ApiResult<Quote> newVersion(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        var user = AuthContext.require();
        Quote existing = find(id);
        Quote copy = new Quote();
        copy.setTenantId(user.getTenantId());
        copy.setQuoteNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "QUOTE"));
        copy.setDemandId(existing.getDemandId());
        copy.setSolutionId(existing.getSolutionId());
        copy.setCustomerId(existing.getCustomerId());
        copy.setCustomerName(existing.getCustomerName());
        copy.setVersion(quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getDemandId, existing.getDemandId())).stream().map(Quote::getVersion)
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 1);
        copy.setProductId(existing.getProductId());
        copy.setProductName(existing.getProductName());
        copy.setUnitPrice(existing.getUnitPrice());
        copy.setQuantity(existing.getQuantity());
        copy.setTaxRate(existing.getTaxRate());
        copy.setFreight(existing.getFreight());
        copy.setInstallFee(existing.getInstallFee());
        copy.setPaymentTerms(existing.getPaymentTerms());
        copy.setWarranty(existing.getWarranty());
        copy.setValidUntil(LocalDate.now().plusDays(30));
        copy.setFloorPrice(existing.getFloorPrice());
        copy.setStatus("DRAFT");
        copy.setOwnerId(user.getId());
        copy.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        copy.setNegotiationCount(0);
        quoteFlowService.recalculate(copy);
        applyApproval(copy);
        quoteMapper.insert(copy);
        auditService.log("报价议价", "新版本", existing.getQuoteNo(), copy.getQuoteNo());
        return ApiResult.ok(copy);
    }

    @GetMapping("/{id}/negotiations")
    public ApiResult<List<Negotiation>> negotiations(@PathVariable Long id) {
        find(id);
        return ApiResult.ok(negotiationMapper.selectList(new LambdaQueryWrapper<Negotiation>()
                .eq(Negotiation::getQuoteId, id).orderByAsc(Negotiation::getRoundNo)));
    }

    @PostMapping("/{id}/negotiate")
    public ApiResult<Negotiation> negotiate(@PathVariable Long id, @RequestBody Negotiation negotiation) {
        var user = AuthContext.require();
        Quote quote = find(id);
        if (negotiation.getAmount() == null) throw new BizException("议价金额不能为空");
        int round = negotiationMapper.selectList(new LambdaQueryWrapper<Negotiation>()
                .eq(Negotiation::getQuoteId, id)).size() + 1;
        negotiation.setId(null);
        negotiation.setTenantId(user.getTenantId());
        negotiation.setQuoteId(id);
        negotiation.setRoundNo(round);
        negotiation.setOperator(user.getName());
        negotiationMapper.insert(negotiation);
        quote.setNegotiationCount(round);
        if ("COMPANY".equals(negotiation.getParty())) {
            quote.setTotalAmount(negotiation.getAmount());
            quote.setAmount(negotiation.getAmount().subtract(quote.getFreight() == null ? BigDecimal.ZERO : quote.getFreight())
                    .subtract(quote.getInstallFee() == null ? BigDecimal.ZERO : quote.getInstallFee()));
            applyApproval(quote);
        } else {
            quote.setStatus("BARGAINING");
        }
        quoteMapper.updateById(quote);
        auditService.log("报价议价", "议价记录", quote.getQuoteNo(), negotiation.getParty() + " " + negotiation.getAmount());
        return ApiResult.ok(negotiation);
    }

    @PostMapping("/{id}/approval")
    public ApiResult<Quote> submitApproval(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        var user = AuthContext.require();
        Quote quote = find(id);
        if (!quoteFlowService.belowFloor(quote)) throw new BizException("当前报价未低于底价，无需审批");
        quote.setApprovalStatus("PENDING");
        quote.setStatus("APPROVAL");
        quoteMapper.updateById(quote);
        auditService.log("报价议价", "提交审批", quote.getQuoteNo(), "总额 " + quote.getTotalAmount() + " 底价 " + quote.getFloorPrice());
        notificationService.notifyTenant(user.getTenantId(), null, "报价低于底价待审批",
                "报价单 " + quote.getQuoteNo() + " 低于底价，请审批", "QUOTE_APPROVAL", "/quotes");
        return ApiResult.ok(quote);
    }

    @PostMapping("/{id}/approve")
    public ApiResult<Quote> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var user = AuthContext.require();
        if (!user.isAdmin()) throw new BizException(403, "仅管理员可审批低于底价报价");
        Quote quote = find(id);
        if (!"PENDING".equals(quote.getApprovalStatus())) throw new BizException("当前报价无待审批申请");
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", false)));
        quote.setApprovalStatus(approved ? "APPROVED" : "REJECTED");
        quote.setApprovalBy(user.getId());
        quote.setApprovalComment(String.valueOf(body.getOrDefault("comment", "")));
        quote.setStatus(approved ? "DRAFT" : "REJECTED");
        quoteMapper.updateById(quote);
        auditService.log("报价议价", approved ? "审批通过" : "审批驳回", quote.getQuoteNo(),
                quote.getApprovalComment());
        notificationService.notifyTenant(user.getTenantId(), quote.getOwnerId(), approved ? "报价审批通过" : "报价审批驳回",
                quote.getQuoteNo() + "：" + quote.getApprovalComment(), "QUOTE_APPROVAL", "/quotes");
        return ApiResult.ok(quote);
    }

    @PostMapping("/{id}/send")
    public ApiResult<Quote> send(@PathVariable Long id) {
        var user = AuthContext.require();
        Quote quote = find(id);
        if (quoteFlowService.belowFloor(quote) && !"APPROVED".equals(quote.getApprovalStatus())) {
            throw new BizException(403, "报价低于底价，须审批通过后方可发送");
        }
        if (quote.getValidUntil() != null && quote.getValidUntil().isBefore(LocalDate.now())) {
            quote.setStatus("EXPIRED");
            quoteMapper.updateById(quote);
            throw new BizException("报价已过期，请先续期/新版本");
        }
        quote.setStatus("SENT");
        quote.setSentTime(java.time.LocalDateTime.now());
        quoteMapper.updateById(quote);
        String link = "/share/quote?token=" + quote.getShareToken();
        if (quote.getCustomerId() != null) {
            notificationService.notifyCustomer(user.getTenantId(), quote.getCustomerId(), "QUOTE_REMIND",
                    "您有新的报价待确认", "报价单 " + quote.getQuoteNo() + "，金额 " + quote.getTotalAmount()
                            + " 元，有效期至 " + quote.getValidUntil(), Map.of("quoteNo", quote.getQuoteNo(),
                            "amount", String.valueOf(quote.getTotalAmount()), "validUntil", String.valueOf(quote.getValidUntil())), link);
        }
        auditService.log("报价议价", "发送", quote.getQuoteNo(), null);
        return ApiResult.ok(quote);
    }

    @PostMapping("/{id}/confirm")
    public ApiResult<Quote> confirm(@PathVariable Long id) {
        var user = AuthContext.require();
        Quote quote = find(id);
        quoteFlowService.confirm(quote, false);
        auditService.log("报价议价", "确认报价", quote.getQuoteNo(), null);
        return ApiResult.ok(quoteMapper.selectById(id));
    }

    @PostMapping("/{id}/convert-contract")
    public ApiResult<Contract> convertContract(@PathVariable Long id) {
        Quote quote = find(id);
        if (!"CONFIRMED".equals(quote.getStatus())) throw new BizException("仅已确认报价可生成合同");
        Contract contract = quoteFlowService.createContractFromQuote(quote, AuthContext.userId());
        auditService.log("报价议价", "生成合同草稿", quote.getQuoteNo(), contract.getContractNo());
        return ApiResult.ok(contract);
    }

    @PostMapping("/expire-scan")
    public ApiResult<Map<String, Object>> expireScan() {
        int count = quoteFlowService.expireQuotes();
        return ApiResult.ok(Map.of("expired", count));
    }

    @GetMapping("/{id}/pdf")
    public void pdf(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Quote quote = find(id);
        List<String[]> table = new ArrayList<>();
        table.add(new String[]{"产品型号", "数量", "单价", "金额", "运费", "安装调试", "含税合计"});
        table.add(new String[]{String.valueOf(quote.getProductName()), String.valueOf(quote.getQuantity()),
                String.valueOf(quote.getUnitPrice()), String.valueOf(quote.getAmount()),
                String.valueOf(quote.getFreight()), String.valueOf(quote.getInstallFee()), String.valueOf(quote.getTotalAmount())});
        byte[] bytes = quoteFlowService.quotePdf(quote, table);
        download(response, bytes, quote.getQuoteNo() + ".pdf");
        auditService.log("报价议价", "PDF下载", quote.getQuoteNo(), null);
    }

    private void download(HttpServletResponse response, byte[] bytes, String name) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
        response.getOutputStream().write(bytes);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Quote quote = find(id);
        if (contractMapper.selectCount(new LambdaQueryWrapper<Contract>().eq(Contract::getQuoteId, id)) > 0) {
            throw new BizException(409, "该报价已生成合同，请先删除合同后再删除报价");
        }
        jdbcTemplate.update("DELETE FROM negotiation WHERE quote_id = ?", id);
        jdbcTemplate.update("DELETE FROM quote WHERE id = ?", id);
        auditService.log("报价议价", "删除", quote.getQuoteNo(), quote.getCustomerName());
        return ApiResult.ok();
    }

    private void applyApproval(Quote quote) {
        if (quoteFlowService.belowFloor(quote)) {
            quote.setApprovalStatus("PENDING");
            quote.setStatus("APPROVAL");
        } else {
            quote.setApprovalStatus("NONE");
            quote.setStatus("DRAFT");
        }
    }

    private String resolveCustomer(Quote quote) {
        if (quote.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(quote.getCustomerId());
            if (customer == null) throw new BizException(404, "客户不存在");
            return customer.getName();
        }
        if (quote.getDemandId() != null) {
            Demand demand = demandMapper.selectById(quote.getDemandId());
            if (demand != null) {
                quote.setCustomerId(demand.getCustomerId());
                return demand.getContactName();
            }
        }
        return quote.getCustomerName();
    }

    private Quote find(Long id) {
        Quote quote = quoteMapper.selectById(id);
        if (quote == null) throw new BizException(404, "报价单不存在");
        return quote;
    }
}
