package com.xinhuo.boiler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.entity.ContractTemplate;
import com.xinhuo.boiler.mapper.ContractTemplateMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/contract-templates")
public class ContractTemplateAdminController extends SimpleCrudController<ContractTemplate> {
    public ContractTemplateAdminController(ContractTemplateMapper mapper, AuditService auditService,
                                           ExcelService excelService, ObjectMapper objectMapper) {
        super(mapper, auditService, excelService, objectMapper, "合同模板",
                List.of("name"), List.of("status"), new LinkedHashMap<>());
    }
}
