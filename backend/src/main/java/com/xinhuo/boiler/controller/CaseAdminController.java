package com.xinhuo.boiler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.entity.CaseLibrary;
import com.xinhuo.boiler.mapper.CaseLibraryMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/cases")
public class CaseAdminController extends SimpleCrudController<CaseLibrary> {
    private final JdbcTemplate jdbcTemplate;

    public CaseAdminController(CaseLibraryMapper mapper, AuditService auditService,
                               ExcelService excelService, ObjectMapper objectMapper,
                               JdbcTemplate jdbcTemplate) {
        super(mapper, auditService, excelService, objectMapper, "成功案例",
                List.of("title", "industry", "scene"),
                List.of("industry", "status"), new LinkedHashMap<>());
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void beforeCreate(CaseLibrary entity) {
        entity.setStatus(entity.getStatus() == null || entity.getStatus().isBlank()
                ? "PUBLISHED" : entity.getStatus().trim().toUpperCase());
    }

    @Override
    protected void beforeUpdate(CaseLibrary entity) {
        if (entity.getStatus() != null && !entity.getStatus().isBlank()) {
            entity.setStatus(entity.getStatus().trim().toUpperCase());
        }
    }

    @Override
    protected void deleteEntity(CaseLibrary entity) {
        jdbcTemplate.update("DELETE FROM case_library WHERE id = ?", entity.getId());
    }
}
