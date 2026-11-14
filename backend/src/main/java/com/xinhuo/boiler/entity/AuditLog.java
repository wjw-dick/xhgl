package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    @JsonIgnore
    private Long tenantId;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String target;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
