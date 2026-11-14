package com.xinhuo.boiler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.entity.Knowledge;
import com.xinhuo.boiler.mapper.KnowledgeMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/knowledge")
public class KnowledgeAdminController extends SimpleCrudController<Knowledge> {
    public KnowledgeAdminController(KnowledgeMapper mapper, AuditService auditService,
                                    ExcelService excelService, ObjectMapper objectMapper) {
        super(mapper, auditService, excelService, objectMapper, "服务知识库",
                List.of("title", "keywords", "content"),
                List.of("category", "status"),
                new LinkedHashMap<>() {{
                    put("title", "标题");
                    put("category", "分类");
                    put("keywords", "关键词");
                    put("viewCount", "浏览量");
                    put("status", "状态");
                }});
    }
}
