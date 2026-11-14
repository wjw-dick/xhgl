package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_plan")
public class PaymentPlan extends BaseEntity {
    private Long contractId;
    private String phase;
    private BigDecimal amount;
    private LocalDate planDate;
    private LocalDate receivedDate;
    private BigDecimal receivedAmount;
    private String status;
    private String remark;
}
