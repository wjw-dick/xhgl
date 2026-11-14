package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("negotiation")
public class Negotiation extends BaseEntity {
    private Long quoteId;
    private Integer roundNo;
    private String party;
    private BigDecimal amount;
    private String remark;
    private String operator;
}
