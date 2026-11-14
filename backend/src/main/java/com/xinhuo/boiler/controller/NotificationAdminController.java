package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {
    private final NotificationMapper notificationMapper;
    private final TemplateMessageConfigMapper templateMapper;
    private final NotifyLogMapper logMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<PageResult<Notification>> list(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Integer readFlag) {
        var user = AuthContext.require();
        Page<Notification> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserType, "ADMIN");
        if (!user.isAdmin()) qw.eq(Notification::getUserId, user.getId());
        if (readFlag != null) qw.eq(Notification::getReadFlag, readFlag);
        qw.orderByDesc(Notification::getId);
        return ApiResult.ok(PageResult.of(notificationMapper.selectPage(p, qw)));
    }

    @GetMapping("/unread-count")
    public ApiResult<Map<String, Object>> unreadCount() {
        var user = AuthContext.require();
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserType, "ADMIN").eq(Notification::getReadFlag, 0);
        if (!user.isAdmin()) qw.eq(Notification::getUserId, user.getId());
        return ApiResult.ok(Map.of("count", notificationMapper.selectCount(qw)));
    }

    @PostMapping("/{id}/read")
    public ApiResult<Void> read(@PathVariable Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) throw new BizException(404, "消息不存在");
        notification.setReadFlag(1);
        notification.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(notification);
        return ApiResult.ok();
    }

    @PostMapping("/read-all")
    public ApiResult<Map<String, Object>> readAll() {
        var user = AuthContext.require();
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReadFlag, 0).eq(Notification::getUserType, "ADMIN");
        if (!user.isAdmin()) qw.eq(Notification::getUserId, user.getId());
        List<Notification> list = notificationMapper.selectList(qw);
        for (Notification n : list) {
            n.setReadFlag(1);
            n.setReadTime(LocalDateTime.now());
            notificationMapper.updateById(n);
        }
        return ApiResult.ok(Map.of("updated", list.size()));
    }

    @GetMapping("/configs")
    public ApiResult<List<TemplateMessageConfig>> configs() {
        return ApiResult.ok(templateMapper.selectList(new LambdaQueryWrapper<TemplateMessageConfig>()
                .orderByAsc(TemplateMessageConfig::getId)));
    }

    @PutMapping("/configs/{id}")
    public ApiResult<TemplateMessageConfig> saveConfig(@PathVariable Long id, @RequestBody TemplateMessageConfig config) {
        TemplateMessageConfig existing = templateMapper.selectById(id);
        if (existing == null) throw new BizException(404, "模板配置不存在");
        existing.setTemplateId(config.getTemplateId());
        existing.setTitle(config.getTitle());
        existing.setEnabled(config.getEnabled());
        existing.setFieldsJson(config.getFieldsJson());
        existing.setMinIntervalSeconds(config.getMinIntervalSeconds());
        existing.setJumpUrl(config.getJumpUrl());
        templateMapper.updateById(existing);
        auditService.log("消息配置", "保存模板", existing.getBizType(), existing.getTemplateId());
        return ApiResult.ok(existing);
    }

    @PostMapping("/test")
    public ApiResult<Map<String, Object>> test(@RequestBody Map<String, Object> body) {
        var user = AuthContext.require();
        String bizType = String.valueOf(body.getOrDefault("bizType", "QUOTE_REMIND"));
        Long customerId = body.get("customerId") == null ? null : Long.valueOf(String.valueOf(body.get("customerId")));
        if (customerId == null) throw new BizException("请选择测试客户");
        notificationService.notifyCustomer(user.getTenantId(), customerId, bizType, "测试消息",
                "这是一条消息通道测试，请忽略。", Map.of("test", "1"), "/account");
        auditService.log("消息配置", "测试发送", bizType, String.valueOf(customerId));
        return ApiResult.ok(Map.of("success", true, "message", "已入队/发送，详见发送日志"));
    }

    @GetMapping("/logs")
    public ApiResult<PageResult<NotifyLog>> logs(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String status) {
        Page<NotifyLog> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<NotifyLog> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) qw.eq(NotifyLog::getStatus, status);
        qw.orderByDesc(NotifyLog::getId);
        return ApiResult.ok(PageResult.of(logMapper.selectPage(p, qw)));
    }
}
