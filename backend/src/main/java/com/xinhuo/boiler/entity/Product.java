package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {
    private String code;
    private String model;
    private String boilerType;
    private String fuelType;
    private String capacity;
    private String pressureLevel;
    private String usageDesc;
    private String techParams;
    private String images;
    private BigDecimal guidePrice;
    private String onlineStatus;
    private String complianceStatus;
    private String certSummary;
    private String remark;
}
