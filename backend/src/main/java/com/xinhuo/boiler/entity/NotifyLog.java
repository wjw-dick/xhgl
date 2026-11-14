package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_log")
public class NotifyLog extends BaseEntity {
    private String channel;
    private String bizType;
    private Long customerId;
    private String target;
    private String templateId;
    private String status;
    private String result;
    private Integer retryCount;
}
