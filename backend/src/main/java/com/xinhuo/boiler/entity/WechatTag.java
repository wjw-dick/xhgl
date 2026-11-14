package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wechat_tag")
public class WechatTag extends BaseEntity {
    private String name;
    private Long wxTagId;
    private Integer fanCount;
}
