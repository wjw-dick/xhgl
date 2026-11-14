package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_tenant")
public class SysDictTenant extends BaseEntity {
    private String dictType;
    private String dictKey;
    private String dictValue;
    private Integer sort;
    private String status;
    private String remark;
}
