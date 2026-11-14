package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spare_record")
public class SpareRecord extends BaseEntity {
    private Long partId;
    private String type;
    private Integer quantity;
    private Long ticketId;
    private String operator;
    private String remark;
    private Integer stockAfter;
}
