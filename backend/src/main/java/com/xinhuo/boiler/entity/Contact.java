package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contact")
public class Contact extends BaseEntity {
    private Long customerId;
    private String name;
    private String title;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String phoneEnc;
    private String phoneMask;
    private String email;
    private String wechatOpenid;
    private String unionid;
    private Integer primaryFlag;
}
