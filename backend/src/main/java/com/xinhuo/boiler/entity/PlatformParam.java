package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("platform_param")
public class PlatformParam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paramKey;
    private String paramValue;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
