package com.xinhuo.boiler.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration expire;

    public JwtService(@Value("${app.jwt-secret}") String secret,
                      @Value("${app.jwt-expire-hours:24}") long hours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = Duration.ofHours(hours);
    }

    public String create(AuthUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", user.getUserType());
        claims.put("tenantId", user.getTenantId());
        claims.put("tenantCode", user.getTenantCode());
        claims.put("tenantName", user.getTenantName());
        claims.put("roleCode", user.getRoleCode());
        claims.put("name", user.getName());
        claims.put("customerId", user.getCustomerId());
        claims.put("permissions", user.getPermissions());
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire.toMillis()))
                .signWith(key)
                .compact();
    }

    public AuthUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return AuthUser.builder()
                .id(Long.valueOf(claims.getSubject()))
                .username(null)
                .name(claims.get("name", String.class))
                .userType(claims.get("userType", String.class))
                .tenantId(claims.get("tenantId") == null ? null : ((Number) claims.get("tenantId")).longValue())
                .tenantCode(claims.get("tenantCode", String.class))
                .tenantName(claims.get("tenantName", String.class))
                .roleCode(claims.get("roleCode", String.class))
                .customerId(claims.get("customerId") == null ? null : ((Number) claims.get("customerId")).longValue())
                .permissions(claims.get("permissions", String.class))
                .build();
    }
}
