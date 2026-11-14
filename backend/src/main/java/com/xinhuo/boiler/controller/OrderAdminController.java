package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {
    private final SalesOrderMapper orderMapper;
    private final ContractMapper contractMapper;
    private final IdGenerator idGenerator;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<PageResult<SalesOrder>> list(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) Long contractId) {
        Page<SalesOrder> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<SalesOrder> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) qw.like(SalesOrder::getOrderNo, keyword);
        if (status != null && !status.isBlank()) qw.eq(SalesOrder::getDeliveryStatus, status);
        if (contractId != null) qw.eq(SalesOrder::getContractId, contractId);
        qw.orderByDesc(SalesOrder::getId);
        return ApiResult.ok(PageResult.of(orderMapper.selectPage(p, qw)));
    }

    @PostMapping
    public ApiResult<SalesOrder> create(@RequestBody SalesOrder order) {
        var user = AuthContext.require();
        Contract contract = contractMapper.selectById(order.getContractId());
        if (contract == null) throw new BizException(404, "合同不存在");
        order.setId(null);
        order.setTenantId(user.getTenantId());
        order.setOrderNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "ORDER"));
        order.setProductionProgress(order.getProductionProgress() == null ? "0%" : order.getProductionProgress());
        order.setDeliveryStatus(order.getDeliveryStatus() == null ? "PENDING" : order.getDeliveryStatus());
        orderMapper.insert(order);
        auditService.log("订单", "创建", order.getOrderNo(), contract.getContractNo());
        return ApiResult.ok(order);
    }

    @PutMapping("/{id}")
    public ApiResult<SalesOrder> update(@PathVariable Long id, @RequestBody SalesOrder order) {
        var user = AuthContext.require();
        SalesOrder existing = orderMapper.selectById(id);
        if (existing == null) throw new BizException(404, "订单不存在");
        order.setId(id);
        order.setTenantId(existing.getTenantId());
        order.setOrderNo(existing.getOrderNo());
        order.setContractId(existing.getContractId());
        if ("SHIPPED".equals(order.getDeliveryStatus()) && order.getShippedAt() == null) order.setShippedAt(LocalDateTime.now());
        if ("ARRIVED".equals(order.getDeliveryStatus()) && order.getArrivedAt() == null) order.setArrivedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        Contract contract = contractMapper.selectById(existing.getContractId());
        if (contract != null && contract.getCustomerId() != null) {
            notificationService.notifyCustomer(user.getTenantId(), contract.getCustomerId(), "DELIVERY_NOTICE",
                    "订单进度更新", "合同 " + contract.getContractNo() + "：生产 " + order.getProductionProgress()
                            + "，物流 " + order.getDeliveryStatus(),
                    Map.of("contractNo", contract.getContractNo(), "stage", order.getDeliveryStatus(),
                            "progress", String.valueOf(order.getProductionProgress())), "/account");
        }
        auditService.log("订单", "进度更新", existing.getOrderNo(), order.getProductionProgress());
        return ApiResult.ok(orderMapper.selectById(id));
    }
}
