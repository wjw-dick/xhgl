package com.xinhuo.boiler.controller;

import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/storage")
@RequiredArgsConstructor
public class StorageAdminController {
    private final StorageService storageService;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<Map<String, Object>> config() {
        return ApiResult.ok(storageService.maskedConfig(AuthContext.tenantId()));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/test")
    public ApiResult<Map<String, Object>> test(@RequestBody Map<String, Object> body) {
        String driver = String.valueOf(body.getOrDefault("driver", "LOCAL"));
        Map<String, Object> config = body.get("config") instanceof Map ? (Map<String, Object>) body.get("config") : Map.of();
        Map<String, String> cfg = new LinkedHashMap<>();
        config.forEach((k, v) -> cfg.put(k, v == null ? null : String.valueOf(v)));
        storageService.test(driver, cfg);
        return ApiResult.ok(Map.of("success", true, "message", "连接测试通过"));
    }

    @SuppressWarnings("unchecked")
    @PutMapping
    public ApiResult<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        String driver = String.valueOf(body.getOrDefault("driver", "LOCAL"));
        Map<String, Object> config = body.get("config") instanceof Map ? (Map<String, Object>) body.get("config") : Map.of();
        Map<String, String> cfg = new LinkedHashMap<>();
        config.forEach((k, v) -> cfg.put(k, v == null ? null : String.valueOf(v)));
        storageService.save(AuthContext.tenantId(), driver, cfg);
        auditService.log("对象存储", "保存配置", driver, null);
        return ApiResult.ok(storageService.maskedConfig(AuthContext.tenantId()));
    }
}
