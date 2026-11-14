package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.SparePart;
import com.xinhuo.boiler.entity.SpareRecord;
import com.xinhuo.boiler.mapper.SparePartMapper;
import com.xinhuo.boiler.mapper.SpareRecordMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/spare-records")
@RequiredArgsConstructor
public class SpareStockController {
    private final SparePartMapper partMapper;
    private final SpareRecordMapper recordMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<PageResult<SpareRecord>> list(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long partId,
                                                   @RequestParam(required = false) String type) {
        Page<SpareRecord> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<SpareRecord> qw = new LambdaQueryWrapper<>();
        if (partId != null) qw.eq(SpareRecord::getPartId, partId);
        if (type != null && !type.isBlank()) qw.eq(SpareRecord::getType, type);
        qw.orderByDesc(SpareRecord::getId);
        return ApiResult.ok(PageResult.of(recordMapper.selectPage(p, qw)));
    }

    @PostMapping("/operation")
    @Transactional
    public ApiResult<SpareRecord> operation(@RequestBody Map<String, Object> body) {
        var user = AuthContext.require();
        Long partId = Long.valueOf(String.valueOf(body.get("partId")));
        String type = String.valueOf(body.get("type"));
        int quantity = Integer.parseInt(String.valueOf(body.getOrDefault("quantity", 1)));
        if (quantity <= 0) throw new BizException("数量必须大于0");
        SparePart part = partMapper.selectById(partId);
        if (part == null) throw new BizException(404, "备件不存在");
        int stock = part.getStock() == null ? 0 : part.getStock();
        switch (type) {
            case "IN" -> stock += quantity;
            case "OUT" -> {
                if (stock < quantity) throw new BizException("库存不足，当前库存 " + stock);
                stock -= quantity;
            }
            case "STOCKTAKE" -> stock = quantity;
            default -> throw new BizException("不支持的库存操作类型");
        }
        part.setStock(stock);
        partMapper.updateById(part);
        SpareRecord record = new SpareRecord();
        record.setTenantId(user.getTenantId());
        record.setPartId(partId);
        record.setType(type);
        record.setQuantity(quantity);
        record.setTicketId(body.get("ticketId") == null ? null : Long.valueOf(String.valueOf(body.get("ticketId"))));
        record.setOperator(user.getName());
        record.setRemark(String.valueOf(body.getOrDefault("remark", "")));
        record.setStockAfter(stock);
        recordMapper.insert(record);
        if (part.getSafetyStock() != null && stock < part.getSafetyStock()) {
            notificationService.notifyTenant(user.getTenantId(), null, "备件库存预警",
                    part.getName() + "（" + part.getCode() + "）库存 " + stock + " 低于安全库存 " + part.getSafetyStock(),
                    "SPARE_LOW", "/spares");
        }
        auditService.log("备品备件", "库存操作", part.getCode(), type + " " + quantity + "，结存 " + stock);
        return ApiResult.ok(record);
    }

    @GetMapping("/alerts")
    public ApiResult<List<SparePart>> alerts() {
        List<SparePart> parts = partMapper.selectList(new LambdaQueryWrapper<SparePart>());
        return ApiResult.ok(parts.stream().filter(p -> p.getStock() != null && p.getSafetyStock() != null
                && p.getStock() < p.getSafetyStock()).toList());
    }
}
