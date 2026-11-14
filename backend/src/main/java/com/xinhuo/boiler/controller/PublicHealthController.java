package com.xinhuo.boiler.controller;

import com.xinhuo.boiler.common.ApiResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicHealthController {
    private final JdbcTemplate jdbcTemplate;

    public PublicHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Integer db = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("database", db != null && db == 1 ? "UP" : "DOWN");
        result.put("time", LocalDateTime.now());
        result.put("version", "1.0.0");
        return ApiResult.ok(result);
    }
}
