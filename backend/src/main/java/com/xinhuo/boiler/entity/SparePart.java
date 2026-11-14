package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spare_part")
public class SparePart extends BaseEntity {
    private String code;
    private String name;
    private String model;
    private String spec;
    private String unit;
    private String supplier;
    private BigDecimal unitPrice;
    private Integer stock;
    private Integer safetyStock;
    private String image;
    private String status;
}
