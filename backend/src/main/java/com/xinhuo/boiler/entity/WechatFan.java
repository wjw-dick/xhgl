package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wechat_fan")
public class WechatFan extends BaseEntity {
    private String openid;
    private String unionid;
    private String nickname;
    private String tags;
    private Integer subscribed;
    private Long bindCustomerId;
}
