package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.PageResult;
import com.xinhuo.boiler.entity.AuditLog;
import com.xinhuo.boiler.mapper.AuditLogMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audits")
@RequiredArgsConstructor
public class AuditAdminController {
    private final AuditLogMapper auditMapper;
    private final ExcelService excelService;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<PageResult<AuditLog>> list(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String module,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String username) {
        Page<AuditLog> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<AuditLog> qw = new LambdaQueryWrapper<>();
        if (module != null && !module.isBlank()) qw.eq(AuditLog::getModule, module);
        if (username != null && !username.isBlank()) qw.like(AuditLog::getUsername, username);
        if (keyword != null && !keyword.isBlank()) qw.and(w -> w.like(AuditLog::getAction, keyword)
                .or().like(AuditLog::getTarget, keyword).or().like(AuditLog::getDetail, keyword));
        qw.orderByDesc(AuditLog::getId);
        return ApiResult.ok(PageResult.of(auditMapper.selectPage(p, qw)));
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response, @RequestParam(required = false) String module) throws IOException {
        LambdaQueryWrapper<AuditLog> qw = new LambdaQueryWrapper<>();
        if (module != null && !module.isBlank()) qw.eq(AuditLog::getModule, module);
        qw.orderByDesc(AuditLog::getId);
        List<AuditLog> logs = auditMapper.selectList(qw);
        List<List<Object>> rows = new ArrayList<>();
        for (AuditLog log : logs) {
            rows.add(java.util.Arrays.asList(String.valueOf(log.getCreatedAt()), log.getUsername(), log.getModule(),
                    log.getAction(), String.valueOf(log.getTarget()), log.getIp(), String.valueOf(log.getDetail())));
        }
        excelService.export(response, "审计日志.xlsx",
                List.of("时间", "操作人", "模块", "动作", "对象", "IP", "详情"), rows);
        auditService.log("审计日志", "导出", null, "共" + logs.size() + "条");
    }

    @GetMapping("/modules")
    public ApiResult<List<String>> modules() {
        List<AuditLog> logs = auditMapper.selectList(new LambdaQueryWrapper<AuditLog>().select(AuditLog::getModule));
        return ApiResult.ok(logs.stream().map(AuditLog::getModule).distinct().toList());
    }
}
