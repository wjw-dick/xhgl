package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
public class Customer extends BaseEntity {
    private String customerNo;
    private String name;
    private String industry;
    private String scale;
    private String region;
    private String address;
    private String source;
    private String category;
    private String level;
    private String status;
    private Long ownerId;
    private String remark;
}
