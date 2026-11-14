package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("satisfaction")
public class Satisfaction extends BaseEntity {
    private Long ticketId;
    private Long contractId;
    private Long customerId;
    private Integer score;
    private String comment;
    private String source;
}
