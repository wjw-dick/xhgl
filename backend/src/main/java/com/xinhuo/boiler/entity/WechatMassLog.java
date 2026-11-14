package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wechat_mass_log")
public class WechatMassLog extends BaseEntity {
    private String targetType;
    private String tagId;
    private String openids;
    private String contentType;
    private String content;
    private String mediaId;
    private String status;
    private String result;
}
