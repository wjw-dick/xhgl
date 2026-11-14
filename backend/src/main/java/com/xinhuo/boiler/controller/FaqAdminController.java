package com.xinhuo.boiler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.entity.Faq;
import com.xinhuo.boiler.mapper.FaqMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/faqs")
public class FaqAdminController extends SimpleCrudController<Faq> {
    public FaqAdminController(FaqMapper mapper, AuditService auditService,
                              ExcelService excelService, ObjectMapper objectMapper) {
        super(mapper, auditService, excelService, objectMapper, "FAQ知识库",
                List.of("question", "keywords"), List.of("category", "status"), new LinkedHashMap<>());
    }
}
