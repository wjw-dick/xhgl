package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("share_log")
public class ShareLog extends BaseEntity {
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String source;
    private String ip;
    private String userAgent;
}
