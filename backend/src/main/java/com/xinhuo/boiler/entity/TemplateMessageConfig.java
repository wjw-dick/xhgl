package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("template_message_config")
public class TemplateMessageConfig extends BaseEntity {
    private String bizType;
    private String bizName;
    private String templateId;
    private String title;
    private Integer enabled;
    private String fieldsJson;
    private Integer minIntervalSeconds;
    private String jumpUrl;
}
