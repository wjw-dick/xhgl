package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wechat_config")
public class WechatConfig extends BaseEntity {
    private String mpAppid;
    private String mpSecretEnc;
    private String mpToken;
    private String mpAesKeyEnc;
    private String mpServerUrl;
    private String mpRequestDomain;
    private String mpUploadDomain;
    private String mpDownloadDomain;
    private String oaAppid;
    private String oaSecretEnc;
    private String oaServerUrl;
    private String oaToken;
    private String oaAesKeyEnc;
    private String oauthDomain;
    private String jsDomain;
    private String openPlatformAppid;
    private String welcomeText;
    private String keywordReply;
    private String menuJson;
}
