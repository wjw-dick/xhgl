package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.Equipment;
import com.xinhuo.boiler.mapper.EquipmentMapper;
import com.xinhuo.boiler.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/equipment")
@RequiredArgsConstructor
public class EquipmentAdminController {
    private final EquipmentMapper equipmentMapper;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<PageResult<Equipment>> list(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Long customerId) {
        Page<Equipment> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Equipment> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) qw.and(w -> w.like(Equipment::getEquipmentNo, keyword)
                .or().like(Equipment::getModel, keyword).or().like(Equipment::getInstallAddr, keyword));
        if (customerId != null) qw.eq(Equipment::getCustomerId, customerId);
        qw.orderByDesc(Equipment::getId);
        return ApiResult.ok(PageResult.of(equipmentMapper.selectPage(p, qw)));
    }

    @PostMapping
    public ApiResult<Equipment> create(@RequestBody Equipment equipment) {
        var user = AuthContext.require();
        equipment.setId(null);
        equipment.setTenantId(user.getTenantId());
        equipmentMapper.insert(equipment);
        auditService.log("设备台账", "新增", equipment.getEquipmentNo(), equipment.getModel());
        return ApiResult.ok(equipment);
    }

    @PutMapping("/{id}")
    public ApiResult<Equipment> update(@PathVariable Long id, @RequestBody Equipment equipment) {
        Equipment existing = equipmentMapper.selectById(id);
        if (existing == null) throw new BizException(404, "设备不存在");
        equipment.setId(id);
        equipment.setTenantId(existing.getTenantId());
        equipment.setEquipmentNo(existing.getEquipmentNo());
        equipmentMapper.updateById(equipment);
        return ApiResult.ok(equipmentMapper.selectById(id));
    }
}
