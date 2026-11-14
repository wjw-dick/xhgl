package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.entity.Contract;
import com.xinhuo.boiler.entity.PaymentPlan;
import com.xinhuo.boiler.entity.PlatformParam;
import com.xinhuo.boiler.mapper.ContractMapper;
import com.xinhuo.boiler.mapper.PaymentPlanMapper;
import com.xinhuo.boiler.mapper.PlatformParamMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class PaymentAdminController {
    private final PaymentPlanMapper paymentMapper;
    private final ContractMapper contractMapper;
    private final PlatformParamMapper paramMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<List<PaymentPlan>> list(@RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long contractId) {
        LambdaQueryWrapper<PaymentPlan> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) qw.eq(PaymentPlan::getStatus, status);
        if (contractId != null) qw.eq(PaymentPlan::getContractId, contractId);
        qw.orderByAsc(PaymentPlan::getPlanDate);
        return ApiResult.ok(paymentMapper.selectList(qw));
    }

    @GetMapping("/aging")
    public ApiResult<List<Map<String, Object>>> aging() {
        int overdueDaysParam = paramInt("paymentOverdueDays", 7);
        List<PaymentPlan> plans = paymentMapper.selectList(new LambdaQueryWrapper<PaymentPlan>()
                .ne(PaymentPlan::getStatus, "PAID").orderByAsc(PaymentPlan::getPlanDate));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PaymentPlan plan : plans) {
            Contract contract = plan.getContractId() == null ? null : contractMapper.selectById(plan.getContractId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("plan", plan);
            item.put("contract", contract);
            long days = plan.getPlanDate() == null ? 0 : ChronoUnit.DAYS.between(plan.getPlanDate(), LocalDate.now());
            item.put("overdueDays", days);
            String level = days <= 0 ? "NORMAL" : days > overdueDaysParam ? "RED" : "YELLOW";
            item.put("level", level);
            if (days > 0 && !"OVERDUE".equals(plan.getStatus())) {
                plan.setStatus("OVERDUE");
                paymentMapper.updateById(plan);
            }
            result.add(item);
        }
        return ApiResult.ok(result);
    }

    @PostMapping("/remind")
    public ApiResult<Map<String, Object>> remind(@RequestParam(required = false) Long planId) {
        var user = AuthContext.require();
        List<PaymentPlan> plans = paymentMapper.selectList(new LambdaQueryWrapper<PaymentPlan>()
                .ne(PaymentPlan::getStatus, "PAID").lt(PaymentPlan::getPlanDate, LocalDate.now()));
        int sent = 0;
        for (PaymentPlan plan : plans) {
            if (planId != null && !planId.equals(plan.getId())) continue;
            Contract contract = contractMapper.selectById(plan.getContractId());
            if (contract == null) continue;
            if (contract.getCustomerId() != null) {
                notificationService.notifyCustomer(contract.getTenantId(), contract.getCustomerId(), "PAYMENT_REMIND",
                        "回款提醒", "合同 " + contract.getContractNo() + " " + plan.getPhase() + " 已逾期，应付 "
                                + plan.getAmount() + " 元", Map.of("contractNo", contract.getContractNo(),
                                "amount", String.valueOf(plan.getAmount()), "phase", plan.getPhase()), "/account");
            }
            if (contract.getOwnerId() != null) {
                notificationService.notifyTenant(contract.getTenantId(), contract.getOwnerId(), "回款逾期提醒",
                        contract.getContractNo() + " " + plan.getPhase() + " 已逾期", "PAYMENT_OVERDUE", "/contracts");
            }
            sent++;
        }
        auditService.log("回款", "催款提醒", planId == null ? "全部" : String.valueOf(planId), "发送" + sent + "条");
        return ApiResult.ok(Map.of("sent", sent));
    }

    private int paramInt(String key, int defaultValue) {
        PlatformParam param = paramMapper.selectOne(new LambdaQueryWrapper<PlatformParam>()
                .eq(PlatformParam::getParamKey, key).last("limit 1"));
        try {
            return param == null ? defaultValue : Integer.parseInt(param.getParamValue());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
