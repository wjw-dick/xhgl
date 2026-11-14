package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.common.BizException;
import com.xinhuo.boiler.common.PageResult;
import com.xinhuo.boiler.entity.BaseEntity;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

public abstract class SimpleCrudController<T extends BaseEntity> {
    protected final BaseMapper<T> mapper;
    protected final AuditService auditService;
    protected final ExcelService excelService;
    protected final ObjectMapper objectMapper;
    protected final String module;
    protected final List<String> keywordFields;
    protected final List<String> filterFields;
    protected final LinkedHashMap<String, String> exportColumns;

    protected SimpleCrudController(BaseMapper<T> mapper, AuditService auditService, ExcelService excelService,
                                   ObjectMapper objectMapper, String module, List<String> keywordFields,
                                   List<String> filterFields, LinkedHashMap<String, String> exportColumns) {
        this.mapper = mapper;
        this.auditService = auditService;
        this.excelService = excelService;
        this.objectMapper = objectMapper;
        this.module = module;
        this.keywordFields = keywordFields;
        this.filterFields = filterFields;
        this.exportColumns = exportColumns;
    }

    @GetMapping
    public ApiResult<PageResult<T>> list(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         HttpServletRequest request) {
        Page<T> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        QueryWrapper<T> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank() && !keywordFields.isEmpty()) {
            qw.and(w -> {
                for (int i = 0; i < keywordFields.size(); i++) {
                    if (i > 0) w.or();
                    w.like(keywordFields.get(i), keyword);
                }
            });
        }
        for (String field : filterFields) {
            String value = request.getParameter(field);
            if (value != null && !value.isBlank()) {
                qw.eq(field, value);
            }
        }
        applyScope(qw);
        qw.orderByDesc("id");
        return ApiResult.ok(PageResult.of(mapper.selectPage(p, qw)));
    }

    protected void applyScope(QueryWrapper<T> wrapper) {
    }

    @GetMapping("/{id}")
    public ApiResult<T> detail(@PathVariable Long id) {
        T entity = mapper.selectById(id);
        if (entity == null) throw new BizException(404, "记录不存在");
        checkAccess(entity);
        return ApiResult.ok(entity);
    }

    @PostMapping
    public ApiResult<T> create(@RequestBody T entity) {
        var user = AuthContext.require();
        entity.setId(null);
        entity.setTenantId(user.getTenantId());
        vaildTenant(user.getTenantId());
        beforeCreate(entity);
        mapper.insert(entity);
        auditService.log(module, "新增", String.valueOf(entity.getId()), objectMapper.valueToTree(entity).toString());
        return ApiResult.ok(entity);
    }

    @PutMapping("/{id}")
    public ApiResult<T> update(@PathVariable Long id, @RequestBody T entity) {
        T existing = mapper.selectById(id);
        if (existing == null) throw new BizException(404, "记录不存在");
        checkAccess(existing);
        entity.setId(id);
        entity.setTenantId(AuthContext.tenantId());
        beforeUpdate(entity);
        mapper.updateById(entity);
        auditService.log(module, "修改", String.valueOf(id), objectMapper.valueToTree(entity).toString());
        return ApiResult.ok(entity);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        T existing = mapper.selectById(id);
        if (existing == null) throw new BizException(404, "记录不存在");
        checkAccess(existing);
        deleteEntity(existing);
        auditService.log(module, "删除", String.valueOf(id), null);
        return ApiResult.ok();
    }

    protected void deleteEntity(T entity) {
        mapper.deleteById(entity.getId());
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response, @RequestParam(required = false) String keyword,
                       HttpServletRequest request) throws IOException {
        QueryWrapper<T> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank() && !keywordFields.isEmpty()) {
            qw.and(w -> {
                for (int i = 0; i < keywordFields.size(); i++) {
                    if (i > 0) w.or();
                    w.like(keywordFields.get(i), keyword);
                }
            });
        }
        for (String field : filterFields) {
            String value = request.getParameter(field);
            if (value != null && !value.isBlank()) qw.eq(field, value);
        }
        applyScope(qw);
        qw.orderByDesc("id");
        List<T> list = mapper.selectList(qw);
        List<List<Object>> rows = new ArrayList<>();
        for (T item : list) {
            Map<String, Object> map = objectMapper.convertValue(item, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            List<Object> row = new ArrayList<>();
            for (String key : exportColumns.keySet()) row.add(map.get(key));
            rows.add(row);
        }
        excelService.export(response, module + "-导出.xlsx", new ArrayList<>(exportColumns.values()), rows);
        auditService.log(module, "导出", keyword, "共" + list.size() + "条");
    }

    protected void beforeCreate(T entity) {
    }

    protected void beforeUpdate(T entity) {
    }

    protected void checkAccess(T entity) {
    }

    protected void vaildTenant(Long tenantId) {
        if (tenantId == null) throw new BizException(403, "缺少企业上下文");
    }
}
