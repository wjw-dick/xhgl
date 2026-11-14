package com.xinhuo.boiler.service;

import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.common.AuthUser;
import com.xinhuo.boiler.common.TenantContext;
import com.xinhuo.boiler.entity.AuditLog;
import com.xinhuo.boiler.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogMapper auditLogMapper;

    public void log(String module, String action, String target, String detail) {
        AuthUser user = AuthContext.get();
        AuditLog log = new AuditLog();
        log.setTenantId(user != null && user.getTenantId() != null ? user.getTenantId() : 0L);
        log.setUserId(user == null ? null : user.getId());
        log.setUsername(user == null ? "anonymous" : user.getName());
        log.setModule(module);
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail != null && detail.length() > 3000 ? detail.substring(0, 3000) : detail);
        log.setCreatedAt(LocalDateTime.now());
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            log.setIp(forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr());
        }
        auditLogMapper.insert(log);
    }

    public void logForTenant(Long tenantId, String module, String action, String username, String target, String detail) {
        AuditLog log = new AuditLog();
        log.setTenantId(tenantId == null ? 0L : tenantId);
        log.setUsername(username);
        log.setModule(module);
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }
}
