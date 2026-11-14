package com.xinhuo.boiler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.BizException;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuoteFlowService {
    private final QuoteMapper quoteMapper;
    private final ContractMapper contractMapper;
    private final ContractTemplateMapper templateMapper;
    private final CustomerMapper customerMapper;
    private final TenantMapper tenantMapper;
    private final IdGenerator idGenerator;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PdfService pdfService;

    public Contract createContractFromQuote(Quote quote, Long operatorId) {
        if ("EXPIRED".equals(quote.getStatus()) || (quote.getValidUntil() != null && quote.getValidUntil().isBefore(LocalDate.now()))) {
            if (!"EXPIRED".equals(quote.getStatus())) {
                quote.setStatus("EXPIRED");
                quoteMapper.updateById(quote);
            }
            throw new BizException("报价已过期，不可直接成交，请重新报价");
        }
        Contract existing = contractMapper.selectOne(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getQuoteId, quote.getId()).last("limit 1"));
        if (existing != null) {
            return existing;
        }
        Customer customer = quote.getCustomerId() == null ? null : customerMapper.selectById(quote.getCustomerId());
        Tenant tenant = tenantMapper.selectById(quote.getTenantId());
        Contract contract = new Contract();
        contract.setTenantId(quote.getTenantId());
        contract.setContractNo(idGenerator.next(quote.getTenantId(), tenant == null ? "T" : tenant.getCode(), "CONTRACT"));
        contract.setQuoteId(quote.getId());
        contract.setCustomerId(quote.getCustomerId());
        contract.setCustomerName(customer == null ? quote.getCustomerName() : customer.getName());
        contract.setTitle((quote.getProductName() == null ? "锅炉设备" : quote.getProductName()) + " 采购合同");
        contract.setAmount(quote.getTotalAmount());
        contract.setPaymentTerms(quote.getPaymentTerms());
        contract.setTemplateName("标准锅炉销售合同");
        contract.setAuditStatus("PENDING");
        contract.setSignStatus("UNSIGNED");
        contract.setOwnerId(quote.getOwnerId() == null ? operatorId : quote.getOwnerId());
        contract.setStatus("DRAFT");
        ContractTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getStatus, "NORMAL").orderByAsc(ContractTemplate::getId).last("limit 1"));
        String content = template == null ? "<h2>锅炉销售合同</h2>" : template.getContent();
        content = content.replace("{{customerName}}", contract.getCustomerName() == null ? "" : contract.getCustomerName())
                .replace("{{tenantName}}", tenant == null ? "" : tenant.getName())
                .replace("{{model}}", quote.getProductName() == null ? "" : quote.getProductName())
                .replace("{{quantity}}", String.valueOf(quote.getQuantity()))
                .replace("{{amount}}", String.valueOf(quote.getTotalAmount()))
                .replace("{{paymentTerms}}", quote.getPaymentTerms() == null ? "" : quote.getPaymentTerms())
                .replace("{{warranty}}", quote.getWarranty() == null ? "" : quote.getWarranty());
        contract.setContent(content);
        contractMapper.insert(contract);
        auditService.logForTenant(quote.getTenantId(), "报价议价", "生成合同草稿", "系统",
                quote.getQuoteNo(), contract.getContractNo());
        if (quote.getOwnerId() != null) {
            notificationService.notifyTenant(quote.getTenantId(), quote.getOwnerId(), "合同草稿已生成",
                    "报价单 " + quote.getQuoteNo() + " 已生成合同草稿 " + contract.getContractNo(),
                    "CONTRACT_DRAFT", "/contracts");
        }
        return contract;
    }

    public void confirm(Quote quote, boolean customer) {
        if ("EXPIRED".equals(quote.getStatus())) throw new BizException("报价已过期");
        if (quote.getValidUntil() != null && quote.getValidUntil().isBefore(LocalDate.now())) {
            quote.setStatus("EXPIRED");
            quoteMapper.updateById(quote);
            throw new BizException("报价已过期");
        }
        if (!"SENT".equals(quote.getStatus()) && !"BARGAINING".equals(quote.getStatus())
                && !"APPROVED".equals(quote.getStatus()) && !"CONFIRMED".equals(quote.getStatus())) {
            throw new BizException("当前报价状态不可确认");
        }
        quote.setStatus("CONFIRMED");
        quote.setConfirmTime(java.time.LocalDateTime.now());
        quoteMapper.updateById(quote);
        Contract contract = createContractFromQuote(quote, quote.getOwnerId());
        notificationService.notifyTenant(quote.getTenantId(), quote.getOwnerId(), "客户已确认报价",
                "报价单 " + quote.getQuoteNo() + " 已确认，合同 " + contract.getContractNo() + " 已生成草稿",
                "QUOTE_CONFIRMED", "/contracts");
    }

    public int expireQuotes() {
        List<Quote> quotes = quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .in(Quote::getStatus, "DRAFT", "SENT", "BARGAINING", "APPROVAL")
                .lt(Quote::getValidUntil, LocalDate.now()));
        int count = 0;
        for (Quote quote : quotes) {
            quote.setStatus("EXPIRED");
            quoteMapper.updateById(quote);
            count++;
            if (quote.getOwnerId() != null) {
                notificationService.notifyTenant(quote.getTenantId(), quote.getOwnerId(), "报价已过期",
                        "报价单 " + quote.getQuoteNo() + " 已过有效期，需重新报价", "QUOTE_EXPIRE", "/quotes");
            }
        }
        return count;
    }

    public byte[] quotePdf(Quote quote, List<String[]> table) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("报价单号", quote.getQuoteNo());
        fields.put("客户", quote.getCustomerName());
        fields.put("产品型号", quote.getProductName());
        fields.put("含税总价", "¥" + quote.getTotalAmount());
        fields.put("付款条件", quote.getPaymentTerms());
        fields.put("质保期", quote.getWarranty());
        fields.put("有效期至", String.valueOf(quote.getValidUntil()));
        fields.put("状态", quote.getStatus());
        return pdfService.build("锅炉设备报价单", "报价单号：" + quote.getQuoteNo(), fields, table,
                List.of("备注：本报价含运费与安装调试费用，最终以双方签署合同为准。",
                        "底价红线：低于底价须审批通过后方可发送。"),
                "报价方（盖章）：______________________        客户确认：______________________");
    }

    public BigDecimal recalculate(Quote quote) {
        BigDecimal unit = quote.getUnitPrice() == null ? BigDecimal.ZERO : quote.getUnitPrice();
        BigDecimal qty = BigDecimal.valueOf(quote.getQuantity() == null ? 1 : quote.getQuantity());
        BigDecimal amount = unit.multiply(qty);
        BigDecimal total = amount
                .add(quote.getFreight() == null ? BigDecimal.ZERO : quote.getFreight())
                .add(quote.getInstallFee() == null ? BigDecimal.ZERO : quote.getInstallFee());
        quote.setAmount(amount);
        quote.setTotalAmount(total);
        return total;
    }

    public boolean belowFloor(Quote quote) {
        return quote.getFloorPrice() != null && quote.getTotalAmount() != null
                && quote.getTotalAmount().compareTo(quote.getFloorPrice()) < 0;
    }
}
