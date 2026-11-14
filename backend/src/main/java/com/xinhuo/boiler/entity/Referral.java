package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("referral")
public class Referral extends BaseEntity {
    private Long sourceCustomerId;
    private String sourceCustomerName;
    private String targetCompany;
    private String contactName;
    private String phoneMask;
    private String demandDesc;
    private String status;
    private Long ownerId;
    private String reward;
}
