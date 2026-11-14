package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.entity.Tenant;
import com.xinhuo.boiler.entity.WechatConfig;
import com.xinhuo.boiler.entity.WechatFan;
import com.xinhuo.boiler.mapper.TenantMapper;
import com.xinhuo.boiler.mapper.WechatConfigMapper;
import com.xinhuo.boiler.mapper.WechatFanMapper;
import com.xinhuo.boiler.service.CryptoService;
import com.xinhuo.boiler.service.WechatGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/wechat")
@RequiredArgsConstructor
public class WechatCallbackController {
    private final TenantMapper tenantMapper;
    private final WechatConfigMapper configMapper;
    private final WechatFanMapper fanMapper;
    private final CryptoService cryptoService;
    private final WechatGateway wechatGateway;

    @GetMapping("/callback")
    public String verify(@RequestParam String signature,
                         @RequestParam String timestamp,
                         @RequestParam String nonce,
                         @RequestParam String echostr) {
        WechatConfig config = config();
        if (!signature.equals(wechatSignature(config.getOaToken(), timestamp, nonce))) {
            throw new com.xinhuo.boiler.common.BizException(403, "微信签名校验失败");
        }
        return echostr;
    }

    @PostMapping(value = "/callback", produces = MediaType.APPLICATION_XML_VALUE)
    public String callback(@RequestParam(required = false) String signature,
                           @RequestParam(required = false) String msg_signature,
                           @RequestParam String timestamp,
                           @RequestParam String nonce,
                           @RequestParam(required = false, defaultValue = "raw") String encrypt_type,
                           @RequestBody String body) throws Exception {
        WechatConfig config = config();
        String check = msg_signature != null ? msg_signature : signature;
        String plain = body;
        if ("aes".equals(encrypt_type) || body.contains("<Encrypt>")) {
            String encrypted = tag(body, "Encrypt");
            if (check == null || !check.equals(wechatSignature(config.getOaToken(), timestamp, nonce, encrypted))) {
                throw new com.xinhuo.boiler.common.BizException(403, "微信消息签名校验失败");
            }
            plain = decrypt(encrypted, config.getOaAesKeyEnc());
        } else if (signature != null && !signature.equals(wechatSignature(config.getOaToken(), timestamp, nonce))) {
            throw new com.xinhuo.boiler.common.BizException(403, "微信签名校验失败");
        }
        Document document = parse(plain);
        String toUser = tag(plain, "ToUserName");
        String fromUser = tag(plain, "FromUserName");
        String msgType = tag(plain, "MsgType");
        String event = tag(plain, "Event");
        Tenant tenantEntity = companyEntity();
        String reply = null;
        if ("event".equals(msgType)) {
            if ("subscribe".equalsIgnoreCase(event)) {
                WechatFan fan = fanMapper.selectOne(new LambdaQueryWrapper<WechatFan>()
                        .eq(WechatFan::getTenantId, tenantEntity.getId()).eq(WechatFan::getOpenid, fromUser).last("limit 1"));
                if (fan == null) {
                    fan = new WechatFan();
                    fan.setTenantId(tenantEntity.getId());
                    fan.setOpenid(fromUser);
                    fan.setSubscribed(1);
                    fanMapper.insert(fan);
                } else {
                    fan.setSubscribed(1);
                    fanMapper.updateById(fan);
                }
                reply = config.getWelcomeText();
            } else if ("unsubscribe".equalsIgnoreCase(event)) {
                WechatFan fan = fanMapper.selectOne(new LambdaQueryWrapper<WechatFan>()
                        .eq(WechatFan::getTenantId, tenantEntity.getId()).eq(WechatFan::getOpenid, fromUser).last("limit 1"));
                if (fan != null) {
                    fan.setSubscribed(0);
                    fanMapper.updateById(fan);
                }
                return "success";
            }
        } else if ("text".equals(msgType)) {
            String keyword = tag(plain, "Content").trim();
            reply = keywordReply(config, keyword);
        }
        if (reply == null) return "success";
        String xml = replyXml(toUser, fromUser, reply);
        if ("aes".equals(encrypt_type)) {
            return encryptXml(xml, config.getOaToken(), config.getOaAesKeyEnc(), timestamp, nonce, tenantEntity.getCode());
        }
        return xml;
    }

    private String keywordReply(WechatConfig config, String keyword) {
        String portalBase = "https://portal.xinhuo.local";
        String configured = config.getKeywordReply();
        if (configured != null && !configured.isBlank()) {
            for (String entry : configured.split("\\|")) {
                String[] pair = entry.split(":", 2);
                if (pair.length == 2 && keyword.contains(pair[0].trim())) return pair[1].trim();
            }
        }
        if (keyword.contains("需求")) return "请点击提交采购需求：" + portalBase + "/demand";
        if (keyword.contains("报价")) return "请进入客户中心查看报价：" + portalBase + "/account?tab=quotes";
        if (keyword.contains("报修")) return "请进入售后报修：" + portalBase + "/account?tab=service";
        if (keyword.contains("顾问")) return "业务咨询电话：400-800-6688";
        if (keyword.contains("产品")) return "产品中心：" + portalBase + "/products";
        return "欢迎咨询鑫火锅炉。回复“需求”提交采购需求，“报价”查询报价，“报修”进入售后，“产品”查看产品中心。";
    }

    private WechatConfig config() {
        Tenant tenant = companyEntity();
        WechatConfig config = wechatGateway.config(tenant.getId());
        if (config == null) throw new com.xinhuo.boiler.common.BizException(400, "未配置公众号");
        return config;
    }

    private Tenant companyEntity() {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, "HLJ").last("limit 1"));
        if (tenant == null) throw new com.xinhuo.boiler.common.BizException(404, "企业信息不存在");
        return tenant;
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String tag(String xml, String name) {
        try {
            Document doc = parse(xml);
            NodeList nodes = doc.getElementsByTagName(name);
            return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
        } catch (Exception e) {
            return "";
        }
    }

    private String replyXml(String toUser, String fromUser, String content) {
        return "<xml><ToUserName><![CDATA[" + fromUser + "]]></ToUserName>"
                + "<FromUserName><![CDATA[" + toUser + "]]></FromUserName>"
                + "<CreateTime>" + System.currentTimeMillis() / 1000 + "</CreateTime>"
                + "<MsgType><![CDATA[text]]></MsgType>"
                + "<Content><![CDATA[" + content.replace("]]>", "]]]]><![CDATA[>") + "]]></Content></xml>";
    }

    private String wechatSignature(String... parts) {
        String raw = java.util.Arrays.stream(parts).sorted().collect(java.util.stream.Collectors.joining());
        return sha1(raw);
    }

    private String sha1(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String decrypt(String encrypted, String keyEnc) {
        try {
            byte[] key = Base64.getDecoder().decode(cryptoService.decrypt(keyEnc) + "=");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new IvParameterSpec(new byte[16]));
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            int len = plain[16] * 256 * 256 * 256 + plain[17] * 256 * 256 + plain[18] * 256 + plain[19];
            return new String(plain, 20, len, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new com.xinhuo.boiler.common.BizException(400, "微信消息解密失败");
        }
    }

    private String encryptXml(String xml, String token, String keyEnc, String timestamp, String nonce, String appid) {
        try {
            byte[] key = Base64.getDecoder().decode(cryptoService.decrypt(keyEnc) + "=");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(new byte[16]));
            byte[] random = new byte[16];
            new SecureRandom().nextBytes(random);
            byte[] msg = xml.getBytes(StandardCharsets.UTF_8);
            int bodyLen = 20 + msg.length + appid.getBytes().length;
            int pad = (32 - bodyLen % 32) % 32;
            byte[] out = new byte[bodyLen + pad];
            System.arraycopy(random, 0, out, 0, 16);
            out[16] = (byte) (msg.length >> 24);
            out[17] = (byte) (msg.length >> 16);
            out[18] = (byte) (msg.length >> 8);
            out[19] = (byte) msg.length;
            System.arraycopy(msg, 0, out, 20, msg.length);
            byte[] app = appid.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(app, 0, out, 20 + msg.length, app.length);
            byte[] encrypted = cipher.doFinal(out);
            String encrypt = Base64.getEncoder().encodeToString(encrypted);
            String signature = wechatSignature(token, timestamp, nonce, encrypt);
            return "<xml><Encrypt><![CDATA[" + encrypt + "]]></Encrypt>"
                    + "<MsgSignature><![CDATA[" + signature + "]]></MsgSignature>"
                    + "<TimeStamp>" + timestamp + "</TimeStamp><Nonce><![CDATA[" + nonce + "]]></Nonce></xml>";
        } catch (Exception e) {
            return replyXml("", "", "success");
        }
    }
}
