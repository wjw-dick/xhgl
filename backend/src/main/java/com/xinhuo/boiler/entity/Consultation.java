package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("consultation")
public class Consultation extends BaseEntity {
    private Long customerId;
    private Long demandId;
    private String channel;
    private String question;
    private String answer;
    private Long consultantId;
    private String consultantName;
    private String status;
    private Long faqId;
    private String customerName;
    private String contactPhoneMask;
}
