package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_order")
public class SalesOrder extends BaseEntity {
    private String orderNo;
    private Long contractId;
    private String productionProgress;
    private String deliveryStatus;
    private LocalDate expectedDelivery;
    private LocalDateTime shippedAt;
    private LocalDateTime arrivedAt;
    private String remark;
}
