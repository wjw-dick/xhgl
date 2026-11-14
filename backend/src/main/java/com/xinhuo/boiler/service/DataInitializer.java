package com.xinhuo.boiler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> tenantUsers = jdbcTemplate.queryForList(
                "SELECT id, password FROM tenant_user WHERE password LIKE 'plain:%'");
        for (Map<String, Object> row : tenantUsers) {
            String raw = String.valueOf(row.get("password")).substring("plain:".length());
            jdbcTemplate.update("UPDATE tenant_user SET password = ? WHERE id = ?",
                    passwordEncoder.encode(raw), row.get("id"));
        }
    }
}
