package com.xinhuo.boiler.controller;

import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.entity.Tenant;
import com.xinhuo.boiler.entity.TenantUser;
import com.xinhuo.boiler.mapper.TenantMapper;
import com.xinhuo.boiler.mapper.TenantUserMapper;
import com.xinhuo.boiler.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AuthService authService;
    private final TenantMapper tenantMapper;
    private final TenantUserMapper tenantUserMapper;

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return ApiResult.ok(authService.loginAdmin(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        var user = AuthContext.require();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        if (user.getTenantId() != null) {
            Tenant tenant = tenantMapper.selectById(user.getTenantId());
            result.put("company", authService.company(tenant));
        }
        return ApiResult.ok(result);
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.ok();
    }
}
