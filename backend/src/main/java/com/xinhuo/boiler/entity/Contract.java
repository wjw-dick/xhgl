package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity {
    private String contractNo;
    private Long quoteId;
    private Long customerId;
    private String customerName;
    private String title;
    private BigDecimal amount;
    private String paymentTerms;
    private String templateName;
    private String auditStatus;
    private String signStatus;
    private String signType;
    private LocalDateTime signTime;
    private String archiveUrl;
    private Long ownerId;
    private String status;
    private String content;
}
