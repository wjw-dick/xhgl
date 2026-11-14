package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("invoice")
public class Invoice extends BaseEntity {
    private Long contractId;
    private String invoiceNo;
    private String title;
    private String taxNo;
    private BigDecimal amount;
    private String status;
    private LocalDate issuedAt;
    private String remark;
}
