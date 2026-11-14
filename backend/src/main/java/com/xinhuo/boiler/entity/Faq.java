package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("faq")
public class Faq extends BaseEntity {
    private String category;
    private String question;
    private String answer;
    private String keywords;
    private String status;
}
