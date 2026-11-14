package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_ticket")
public class ServiceTicket extends BaseEntity {
    private String ticketNo;
    private Long customerId;
    private String customerName;
    private Long equipmentId;
    private String equipmentNo;
    private String type;
    private String description;
    private String photos;
    private String priority;
    private Long technicianId;
    private String technicianName;
    private String status;
    private LocalDateTime acceptedTime;
    private LocalDateTime doneTime;
    private String solution;
    private Integer satisfactionScore;
    private String review;
    private Long ownerId;
    private String contactName;
    private String contactPhoneMask;
    private String source;
}
