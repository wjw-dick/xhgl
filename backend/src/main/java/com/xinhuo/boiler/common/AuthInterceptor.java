package com.xinhuo.boiler.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !request.getRequestURI().startsWith("/api/")) {
            return true;
        }
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            return reject(response, 401, "未登录或登录已失效");
        }
        AuthUser user;
        try {
            user = jwtService.parse(token);
        } catch (Exception e) {
            return reject(response, 401, "登录令牌无效或已过期");
        }
        user.setReadOnly(false);

        String path = request.getRequestURI();
        if ((path.startsWith("/api/customer/") || path.startsWith("/api/mp/")) && !user.isCustomer()) {
            return reject(response, 403, "仅用户可访问");
        }
        if (path.startsWith("/api/admin/") && !user.isAdmin()) {
            return reject(response, 403, "仅管理员可访问管理端");
        }
        user.setUsername(request.getAttribute("username") instanceof String s ? s : user.getUsername());
        AuthContext.set(user);
        TenantContext.set(user);
        request.setAttribute("authUser", user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
        TenantContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private boolean reject(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(code, message)));
        return false;
    }
}
