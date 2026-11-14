package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quote")
public class Quote extends BaseEntity {
    private String quoteNo;
    private Long demandId;
    private Long solutionId;
    private Long customerId;
    private Integer version;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private BigDecimal freight;
    private BigDecimal installFee;
    private BigDecimal totalAmount;
    private String paymentTerms;
    private String warranty;
    private LocalDate validUntil;
    private BigDecimal floorPrice;
    private String status;
    private String approvalStatus;
    private Long approvalBy;
    private String approvalComment;
    private String shareToken;
    private LocalDateTime confirmTime;
    private Integer negotiationCount;
    private LocalDateTime sentTime;
    private Long ownerId;
    private String customerName;
}
