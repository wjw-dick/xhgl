package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("language")
public class Language extends BaseEntity {
    private String code;
    private String name;
    private Integer enabled;
    private Integer sort;
    private String defaultFlag;
}
