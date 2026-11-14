package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_config")
public class TenantConfig extends BaseEntity {
    private String configKey;
    private String configValue;
    private String groupName;
    private Integer encrypted;
    private String remark;
}
