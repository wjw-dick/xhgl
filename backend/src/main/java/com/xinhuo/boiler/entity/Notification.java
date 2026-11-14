package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {
    private String userType;
    private Long userId;
    private Long customerId;
    private String title;
    private String content;
    private String bizType;
    private Integer readFlag;
    private LocalDateTime readTime;
    private String link;
}
