package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("demand")
public class Demand extends BaseEntity {
    private String demandNo;
    private Long customerId;
    private Long contactId;
    private String contactName;
    private String contactPhoneMask;
    private String boilerType;
    private String fuelType;
    private String capacity;
    private String pressureLevel;
    private String usageDesc;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String region;
    private LocalDate expectTime;
    private String source;
    private Long ownerId;
    private String ownerName;
    private String status;
    private LocalDateTime followNextTime;
    private LocalDateTime lastFollowTime;
    private String description;
    private String shareToken;
    private String assignRemark;
}
