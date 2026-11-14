package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wechat_material")
public class WechatMaterial extends BaseEntity {
    private String title;
    private String mediaType;
    private String mediaId;
    private String content;
    private String url;
    private String status;
}
