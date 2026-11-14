package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("certificate")
public class Certificate extends BaseEntity {
    private String certType;
    private String certNo;
    private String issuer;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Long productId;
    private Long customerId;
    private Long equipmentId;
    private String fileUrl;
    private String status;
    private String remark;
}
