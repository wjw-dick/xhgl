package com.xinhuo.boiler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.entity.CustomerVisit;
import com.xinhuo.boiler.mapper.CustomerVisitMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/visits")
public class VisitAdminController extends SimpleCrudController<CustomerVisit> {
    public VisitAdminController(CustomerVisitMapper mapper, AuditService auditService,
                                ExcelService excelService, ObjectMapper objectMapper) {
        super(mapper, auditService, excelService, objectMapper, "客户回访",
                List.of("customer_name", "content"),
                List.of("status"), new LinkedHashMap<>());
    }

    @Override
    protected void beforeCreate(CustomerVisit entity) {
        if (entity.getOwnerId() == null) entity.setOwnerId(AuthContext.userId());
    }
}
