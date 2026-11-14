package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("case_library")
public class CaseLibrary extends BaseEntity {
    private String title;
    private String industry;
    private String scene;
    private String params;
    private String effect;
    private String images;
    private String content;
    private String status;
}
