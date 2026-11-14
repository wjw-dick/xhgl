package com.xinhuo.boiler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobs {
    private final TenantMapper tenantMapper;
    private final CertificateMapper certificateMapper;
    private final MaintenancePlanMapper maintenanceMapper;
    private final PaymentPlanMapper paymentMapper;
    private final ContractMapper contractMapper;
    private final DemandMapper demandMapper;
    private final PlatformParamMapper paramMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;
    private final QuoteFlowService quoteFlowService;
    private final AuditService auditService;

    @Scheduled(cron = "0 0 2 * * *")
    public void dailyScan() {
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
        int disabledCompanies = 0;
        for (Tenant tenant : tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .in(Tenant::getStatus, "NORMAL", "TRIAL"))) {
            if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
                tenant.setStatus("EXPIRED");
                tenantMapper.updateById(tenant);
                disabledCompanies++;
            }
        }
        log.info("daily scan finished: certs={}, quotes={}, payments={}, companies={}",
                expiredCerts, expiredQuotes, overduePayments, disabledCompanies);
        auditService.logForTenant(0L, "定时任务", "每日扫描", "system", null,
                "证书" + expiredCerts + "，报价" + expiredQuotes + "，回款" + overduePayments + "，企业" + disabledCompanies);
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void maintenanceAndFollowReminders() {
        int maintDays = paramInt("maintenanceWarnDays", 30);
        int demandDays = paramInt("demandFollowTimeoutDays", 2);
        int sent = 0;
        for (MaintenancePlan plan : maintenanceMapper.selectList(new LambdaQueryWrapper<MaintenancePlan>()
                .eq(MaintenancePlan::getStatus, "ACTIVE")
                .le(MaintenancePlan::getNextDate, LocalDate.now().plusDays(maintDays)))) {
            long exists = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getTenantId, plan.getTenantId())
                    .eq(Notification::getBizType, "MAINTENANCE_REMIND")
                    .eq(Notification::getCustomerId, plan.getCustomerId())
                    .like(Notification::getContent, plan.getEquipmentNo())
                    .ge(Notification::getCreatedAt, LocalDateTime.now().minusDays(1)));
            if (exists > 0) continue;
            if (plan.getCustomerId() != null) {
                notificationService.notifyCustomer(plan.getTenantId(), plan.getCustomerId(), "MAINTENANCE_REMIND",
                        "设备维保/定检到期提醒", "设备 " + plan.getEquipmentNo() + " " + plan.getProject()
                                + " 将于 " + plan.getNextDate() + " 到期，请及时安排",
                        Map.of("equipmentNo", plan.getEquipmentNo(), "project", plan.getProject(),
                                "dueDate", String.valueOf(plan.getNextDate())), "/account");
                sent++;
            }
        }
        for (Demand demand : demandMapper.selectList(new LambdaQueryWrapper<Demand>()
                .notIn(Demand::getStatus, "DEAL", "LOST")
                .lt(Demand::getFollowNextTime, LocalDateTime.now()))) {
            long overdue = ChronoUnit.DAYS.between(demand.getFollowNextTime(), LocalDateTime.now());
            if (overdue >= demandDays && demand.getOwnerId() != null) {
                notificationService.notifyTenant(demand.getTenantId(), demand.getOwnerId(), "需求跟进超时",
                        "需求 " + demand.getDemandNo() + " 已超过 " + overdue + " 天未按计划跟进",
                        "DEMAND_FOLLOW", "/demands");
                sent++;
            }
        }
        log.info("reminder job finished, sent={}", sent);
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void certAndTenantExpiryWarnings() {
        int certDays = paramInt("certWarnDays", 90);
        int tenantDays = paramInt("tenantExpireWarnDays", 30);
        for (Certificate cert : certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getStatus, "VALID").le(Certificate::getValidTo, LocalDate.now().plusDays(certDays)))) {
            notificationService.notifyTenant(cert.getTenantId(), null, "资质证书临期预警",
                    cert.getCertType() + " " + cert.getCertNo() + " 将于 " + cert.getValidTo() + " 到期",
                    "CERT_WARNING", "/certificates");
        }
        for (Tenant tenant : tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .in(Tenant::getStatus, "NORMAL", "TRIAL"))) {
            if (tenant.getExpireTime() == null) continue;
            long days = ChronoUnit.DAYS.between(LocalDateTime.now(), tenant.getExpireTime());
            if (days >= 0 && days <= tenantDays) {
                notificationService.notifyTenant(tenant.getId(), null, "企业服务到期预警",
                        "企业服务将于 " + tenant.getExpireTime() + " 到期，请及时续期", "TENANT_EXPIRE", "/settings");
            }
        }
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
