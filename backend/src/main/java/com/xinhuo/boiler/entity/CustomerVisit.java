package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_visit")
public class CustomerVisit extends BaseEntity {
    private Long customerId;
    private String customerName;
    private Long contractId;
    private LocalDate planDate;
    private LocalDateTime visitTime;
    private String content;
    private Integer score;
    private String status;
    private Long ownerId;
}
