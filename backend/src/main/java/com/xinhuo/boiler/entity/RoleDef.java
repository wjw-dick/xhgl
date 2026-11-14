package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("role_def")
public class RoleDef {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String scope;
    private String permissions;
    private Integer sort;
}
