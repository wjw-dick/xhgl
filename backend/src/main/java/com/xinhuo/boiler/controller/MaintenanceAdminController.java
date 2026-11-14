package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.MaintenancePlan;
import com.xinhuo.boiler.entity.PlatformParam;
import com.xinhuo.boiler.mapper.MaintenancePlanMapper;
import com.xinhuo.boiler.mapper.PlatformParamMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin/maintenance")
@RequiredArgsConstructor
public class MaintenanceAdminController {
    private final MaintenancePlanMapper maintenanceMapper;
    private final PlatformParamMapper paramMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<PageResult<MaintenancePlan>> list(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String project,
                                                       @RequestParam(required = false) Boolean warn) {
        Page<MaintenancePlan> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<MaintenancePlan> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) qw.and(w -> w.like(MaintenancePlan::getEquipmentNo, keyword)
                .or().like(MaintenancePlan::getCustomerName, keyword));
        if (status != null && !status.isBlank()) qw.eq(MaintenancePlan::getStatus, status);
        if (project != null && !project.isBlank()) qw.eq(MaintenancePlan::getProject, project);
        if (Boolean.TRUE.equals(warn)) qw.le(MaintenancePlan::getNextDate, LocalDate.now().plusDays(warnDays()));
        qw.orderByAsc(MaintenancePlan::getNextDate);
        return ApiResult.ok(PageResult.of(maintenanceMapper.selectPage(p, qw)));
    }

    @PostMapping
    public ApiResult<MaintenancePlan> create(@RequestBody MaintenancePlan plan) {
        var user = AuthContext.require();
        plan.setId(null);
        plan.setTenantId(user.getTenantId());
        if (plan.getStatus() == null) plan.setStatus("ACTIVE");
        if (plan.getRemindDays() == null) plan.setRemindDays(warnDays());
        maintenanceMapper.insert(plan);
        auditService.log("维保计划", "新增", plan.getEquipmentNo(), plan.getProject());
        return ApiResult.ok(plan);
    }

    @PutMapping("/{id}")
    public ApiResult<MaintenancePlan> update(@PathVariable Long id, @RequestBody MaintenancePlan plan) {
        MaintenancePlan existing = maintenanceMapper.selectById(id);
        if (existing == null) throw new BizException(404, "维保计划不存在");
        plan.setId(id);
        plan.setTenantId(existing.getTenantId());
        maintenanceMapper.updateById(plan);
        return ApiResult.ok(maintenanceMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        maintenanceMapper.deleteById(id);
        auditService.log("维保计划", "删除", String.valueOf(id), null);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/complete")
    public ApiResult<MaintenancePlan> complete(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        var user = AuthContext.require();
        MaintenancePlan plan = maintenanceMapper.selectById(id);
        if (plan == null) throw new BizException(404, "维保计划不存在");
        plan.setLastDoneDate(LocalDate.now());
        int cycle = plan.getCycleMonths() == null || plan.getCycleMonths() <= 0 ? 12 : plan.getCycleMonths();
        plan.setNextDate(LocalDate.now().plusMonths(cycle));
        plan.setStatus("ACTIVE");
        if (body != null && body.get("remark") != null) plan.setRemark(body.get("remark"));
        maintenanceMapper.updateById(plan);
        if (plan.getCustomerId() != null) {
            notificationService.notifyCustomer(user.getTenantId(), plan.getCustomerId(), "MAINTENANCE_REMIND",
                    "维保完成", "设备 " + plan.getEquipmentNo() + " " + plan.getProject() + " 已完成，下次到期 "
                            + plan.getNextDate(), Map.of("equipmentNo", plan.getEquipmentNo(),
                            "project", plan.getProject(), "dueDate", String.valueOf(plan.getNextDate())), "/account");
        }
        auditService.log("维保计划", "完成", plan.getEquipmentNo(), plan.getProject());
        return ApiResult.ok(plan);
    }

    @GetMapping("/warnings")
    public ApiResult<List<Map<String, Object>>> warnings() {
        int days = warnDays();
        List<MaintenancePlan> plans = maintenanceMapper.selectList(new LambdaQueryWrapper<MaintenancePlan>()
                .eq(MaintenancePlan::getStatus, "ACTIVE")
                .le(MaintenancePlan::getNextDate, LocalDate.now().plusDays(days))
                .orderByAsc(MaintenancePlan::getNextDate));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MaintenancePlan plan : plans) {
            long remain = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), plan.getNextDate());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("plan", plan);
            item.put("remainDays", remain);
            item.put("level", remain < 0 ? "RED" : "YELLOW");
            result.add(item);
        }
        return ApiResult.ok(result);
    }

    private int warnDays() {
        PlatformParam param = paramMapper.selectOne(new LambdaQueryWrapper<PlatformParam>()
                .eq(PlatformParam::getParamKey, "maintenanceWarnDays").last("limit 1"));
        try {
            return param == null ? 30 : Integer.parseInt(param.getParamValue());
        } catch (Exception e) {
            return 30;
        }
    }
}
