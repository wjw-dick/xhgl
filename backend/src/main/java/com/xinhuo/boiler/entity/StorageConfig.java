package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("storage_config")
public class StorageConfig extends BaseEntity {
    private String driver;
    private String configJson;
    private Integer active;
    private String status;
}
