package com.xinhuo.boiler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.ContactMapper;
import com.xinhuo.boiler.mapper.NotificationMapper;
import com.xinhuo.boiler.mapper.NotifyLogMapper;
import com.xinhuo.boiler.mapper.TemplateMessageConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final NotifyLogMapper notifyLogMapper;
    private final TemplateMessageConfigMapper templateConfigMapper;
    private final ContactMapper contactMapper;
    private final WechatGateway wechatGateway;
    private final RateLimiter rateLimiter;

    public void station(Long tenantId, String userType, Long userId, Long customerId,
                        String title, String content, String bizType, String link) {
        Notification notification = new Notification();
        notification.setTenantId(tenantId);
        notification.setUserType(userType);
        notification.setUserId(userId);
        notification.setCustomerId(customerId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBizType(bizType);
        notification.setReadFlag(0);
        notification.setLink(link);
        notificationMapper.insert(notification);
    }

    public void notifyTenant(Long tenantId, Long userId, String title, String content, String bizType, String link) {
        station(tenantId, "ADMIN", userId, null, title, content, bizType, link);
    }

    public void notifyCustomer(Long tenantId, Long customerId, String bizType, String title,
                               String content, Map<String, String> templateData, String link) {
        station(tenantId, "CUSTOMER", null, customerId, title, content, bizType, link);
        TemplateMessageConfig config = templateConfigMapper.selectOne(new LambdaQueryWrapper<TemplateMessageConfig>()
                .eq(TemplateMessageConfig::getTenantId, tenantId)
                .eq(TemplateMessageConfig::getBizType, bizType).last("limit 1"));
        if (config == null || config.getEnabled() == null || config.getEnabled() != 1 || config.getTemplateId() == null
                || config.getTemplateId().isBlank()) {
            writeLog(tenantId, customerId, bizType, config, "SKIPPED", "未配置模板ID或未启用，已使用站内消息");
            return;
        }
        int interval = config.getMinIntervalSeconds() == null ? 60 : config.getMinIntervalSeconds();
        try {
            rateLimiter.once("wechat-template:" + tenantId + ":" + customerId + ":" + bizType, interval);
        } catch (Exception e) {
            writeLog(tenantId, customerId, bizType, config, "SKIPPED", "发送限流：" + e.getMessage());
            return;
        }
        Contact contact = contactMapper.selectOne(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId).isNotNull(Contact::getWechatOpenid)
                .ne(Contact::getWechatOpenid, "").orderByDesc(Contact::getPrimaryFlag).last("limit 1"));
        if (contact == null) {
            writeLog(tenantId, customerId, bizType, config, "SKIPPED", "客户未绑定公众号 OpenID，已使用站内消息");
            return;
        }
        Map<String, String> data = templateData == null ? new LinkedHashMap<>() : templateData;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                wechatGateway.sendTemplate(tenantId, contact.getWechatOpenid(), config.getTemplateId(),
                        config.getJumpUrl(), data);
                writeLog(tenantId, customerId, bizType, config, "SUCCESS", "发送成功");
                return;
            } catch (Exception e) {
                if (attempt == 3) {
                    writeLog(tenantId, customerId, bizType, config, "FAILED", "重试3次失败：" + e.getMessage());
                    log.warn("wechat template send failed tenant={} customer={} biz={}", tenantId, customerId, bizType, e);
                }
            }
        }
    }

    private void writeLog(Long tenantId, Long customerId, String bizType, TemplateMessageConfig config,
                          String status, String result) {
        NotifyLog log = new NotifyLog();
        log.setTenantId(tenantId);
        log.setChannel("WECHAT");
        log.setBizType(bizType);
        log.setCustomerId(customerId);
        log.setTemplateId(config == null ? null : config.getTemplateId());
        log.setStatus(status);
        log.setResult(result);
        log.setRetryCount("SUCCESS".equals(status) ? 0 : 3);
        notifyLogMapper.insert(log);
    }
}
