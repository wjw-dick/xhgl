package com.xinhuo.boiler.controller;

import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.service.RateLimiter;
import com.xinhuo.boiler.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicRegisterController {
    private final RegistrationService registrationService;
    private final RateLimiter rateLimiter;

    @PostMapping("/auth/register")
    public ApiResult<Map<String, Object>> registerCustomer(@RequestBody Map<String, String> body,
                                                           HttpServletRequest request) {
        rateLimiter.check("public-register:" + clientIp(request), 5, 600);
        return ApiResult.ok(registrationService.registerCustomer(body));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
