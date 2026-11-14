package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equipment")
public class Equipment extends BaseEntity {
    private String equipmentNo;
    private Long customerId;
    private Long contractId;
    private Long productId;
    private String model;
    private String capacity;
    private String pressureLevel;
    private String installAddr;
    private LocalDate installDate;
    private LocalDate warrantyEnd;
    private String status;
    private String remark;
}
