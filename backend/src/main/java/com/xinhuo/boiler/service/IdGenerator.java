package com.xinhuo.boiler.service;

import com.xinhuo.boiler.common.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class IdGenerator {
    private final JdbcTemplate jdbcTemplate;

    public String next(Long tenantId, String tenantCode, String bizType) {
        String normalized = bizType.toUpperCase();
        jdbcTemplate.update("""
                INSERT INTO id_sequence (tenant_id, biz_type, current_value, created_at, updated_at)
                VALUES (?, ?, 1, NOW(), NOW())
                ON DUPLICATE KEY UPDATE current_value = current_value + 1, updated_at = NOW()
                """, tenantId, normalized);
        Long seq = jdbcTemplate.queryForObject(
                "SELECT current_value FROM id_sequence WHERE tenant_id = ? AND biz_type = ?",
                Long.class, tenantId, normalized);
        String type = typeCode(normalized);
        return "GL-" + tenantCode + "-" + type + "-" + Year.now().getValue() + "-" + String.format("%04d", seq == null ? 1 : seq);
    }

    public String next(String bizType) {
        var user = AuthContext.require();
        return next(user.getTenantId(), user.getTenantCode(), bizType);
    }

    private String typeCode(String type) {
        return switch (type) {
            case "CUSTOMER" -> "CUST";
            case "DEMAND" -> "DEMAND";
            case "SOLUTION" -> "SOLUTION";
            case "QUOTE" -> "QUOTE";
            case "CONTRACT" -> "CONTRACT";
            case "TICKET" -> "TICKET";
            case "EQUIPMENT" -> "EQ";
            case "ORDER" -> "ORDER";
            case "INVOICE" -> "INV";
            case "VISIT" -> "VISIT";
            case "CERT" -> "CERT";
            case "SPARE" -> "SP";
            default -> type;
        };
    }
}
