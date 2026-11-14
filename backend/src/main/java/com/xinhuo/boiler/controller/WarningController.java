package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.common.BizException;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.NotificationService;
import com.xinhuo.boiler.service.QuoteFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/admin/warnings")
@RequiredArgsConstructor
public class WarningController {
    private final CertificateMapper certificateMapper;
    private final MaintenancePlanMapper maintenanceMapper;
    private final PaymentPlanMapper paymentMapper;
    private final ContractMapper contractMapper;
    private final DemandMapper demandMapper;
    private final PlatformParamMapper paramMapper;
    private final NotificationService notificationService;
    private final QuoteFlowService quoteFlowService;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<Map<String, Object>> warnings() {
        int certDays = paramInt("certWarnDays", 90);
        int maintDays = paramInt("maintenanceWarnDays", 30);
        int payDays = paramInt("paymentOverdueDays", 7);
        int demandDays = paramInt("demandFollowTimeoutDays", 2);

        List<Map<String, Object>> certs = new ArrayList<>();
        for (Certificate certificate : certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .in(Certificate::getStatus, "VALID", "WARNING", "EXPIRED").orderByAsc(Certificate::getValidTo))) {
            long remain = certificate.getValidTo() == null ? 9999
                    : ChronoUnit.DAYS.between(LocalDate.now(), certificate.getValidTo());
            if (remain <= certDays) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("certificate", certificate);
                item.put("remainDays", remain);
                item.put("level", remain < 0 ? "RED" : "YELLOW");
                certs.add(item);
            }
        }

        List<Map<String, Object>> maints = new ArrayList<>();
        for (MaintenancePlan plan : maintenanceMapper.selectList(new LambdaQueryWrapper<MaintenancePlan>()
                .eq(MaintenancePlan::getStatus, "ACTIVE").orderByAsc(MaintenancePlan::getNextDate))) {
            long remain = plan.getNextDate() == null ? 9999
                    : ChronoUnit.DAYS.between(LocalDate.now(), plan.getNextDate());
            if (remain <= maintDays) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("plan", plan);
                item.put("remainDays", remain);
                item.put("level", remain < 0 ? "RED" : "YELLOW");
                maints.add(item);
            }
        }

        List<Map<String, Object>> payments = new ArrayList<>();
        for (PaymentPlan plan : paymentMapper.selectList(new LambdaQueryWrapper<PaymentPlan>()
                .ne(PaymentPlan::getStatus, "PAID").orderByAsc(PaymentPlan::getPlanDate))) {
            long overdue = plan.getPlanDate() == null ? 0 : ChronoUnit.DAYS.between(plan.getPlanDate(), LocalDate.now());
            if (overdue > 0 && overdue >= Math.max(1, payDays / 2)) {
                Contract contract = plan.getContractId() == null ? null : contractMapper.selectById(plan.getContractId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("plan", plan);
                item.put("contract", contract);
                item.put("overdueDays", overdue);
                item.put("level", overdue > payDays ? "RED" : "YELLOW");
                payments.add(item);
            }
        }

        List<Map<String, Object>> demandFollows = new ArrayList<>();
        for (Demand demand : demandMapper.selectList(new LambdaQueryWrapper<Demand>()
                .notIn(Demand::getStatus, "DEAL", "LOST").orderByAsc(Demand::getFollowNextTime))) {
            LocalDateTime next = demand.getFollowNextTime();
            if (next == null || next.isAfter(LocalDateTime.now())) continue;
            long overdue = ChronoUnit.DAYS.between(next, LocalDateTime.now());
            if (overdue >= 1) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("demand", demand);
                item.put("overdueDays", overdue);
                item.put("level", overdue > demandDays ? "RED" : "YELLOW");
                demandFollows.add(item);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("certificates", certs);
        result.put("maintenance", maints);
        result.put("payments", payments);
        result.put("demandFollows", demandFollows);
        result.put("total", certs.size() + maints.size() + payments.size() + demandFollows.size());
        return ApiResult.ok(result);
    }

    @PostMapping("/scan")
    public ApiResult<Map<String, Object>> scan() {
        var user = AuthContext.require();
        int expiredCerts = 0;
        for (Certificate cert : certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getStatus, "VALID"))) {
            if (cert.getValidTo() != null && cert.getValidTo().isBefore(LocalDate.now())) {
                cert.setStatus("EXPIRED");
                certificateMapper.updateById(cert);
                expiredCerts++;
            }
        }
        int expiredQuotes = quoteFlowService.expireQuotes();
        int overduePayments = 0;
        for (PaymentPlan plan : paymentMapper.selectList(new LambdaQueryWrapper<PaymentPlan>()
                .notIn(PaymentPlan::getStatus, "PAID", "OVERDUE"))) {
            if (plan.getPlanDate() != null && plan.getPlanDate().isBefore(LocalDate.now())) {
                plan.setStatus("OVERDUE");
                paymentMapper.updateById(plan);
                overduePayments++;
            }
        }
        auditService.log("预警中心", "扫描", null, "证书过期" + expiredCerts + "，报价过期" + expiredQuotes
                + "，回款逾期" + overduePayments);
        return ApiResult.ok(Map.of("expiredCertificates", expiredCerts, "expiredQuotes", expiredQuotes,
                "overduePayments", overduePayments));
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
