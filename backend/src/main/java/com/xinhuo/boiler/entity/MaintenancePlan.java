package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("maintenance_plan")
public class MaintenancePlan extends BaseEntity {
    private Long equipmentId;
    private String equipmentNo;
    private Long customerId;
    private String customerName;
    private String project;
    private Integer cycleMonths;
    private LocalDate nextDate;
    private LocalDate lastDoneDate;
    private String status;
    private Integer remindDays;
    private String remark;
}
