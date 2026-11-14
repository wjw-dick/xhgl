package com.xinhuo.boiler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.common.BizException;
import com.xinhuo.boiler.entity.Tenant;
import com.xinhuo.boiler.entity.WechatConfig;
import com.xinhuo.boiler.mapper.TenantMapper;
import com.xinhuo.boiler.mapper.WechatConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WechatGateway {
    private final WechatConfigMapper wechatConfigMapper;
    private final TenantMapper tenantMapper;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final Map<String, TokenCache> tokenCache = new HashMap<>();

    record TokenCache(String token, long expireAt) {
    }

    public WechatConfig config(Long tenantId) {
        return wechatConfigMapper.selectOne(new LambdaQueryWrapper<WechatConfig>()
                .eq(WechatConfig::getTenantId, tenantId).last("limit 1"));
    }

    public String officialAccessToken(Long tenantId) throws Exception {
        WechatConfig config = config(tenantId);
        if (config == null || blank(config.getOaAppid()) || blank(config.getOaSecretEnc())) {
            throw new BizException("未配置公众号 AppID/AppSecret");
        }
        return accessToken("oa:" + tenantId, config.getOaAppid(), cryptoService.decrypt(config.getOaSecretEnc()), "client_credential");
    }

    public String miniAccessToken(Long tenantId) throws Exception {
        WechatConfig config = config(tenantId);
        if (config == null || blank(config.getMpAppid()) || blank(config.getMpSecretEnc())) {
            throw new BizException("未配置小程序 AppID/AppSecret");
        }
        return accessToken("mp:" + tenantId, config.getMpAppid(), cryptoService.decrypt(config.getMpSecretEnc()), "client_credential");
    }

    public Map<String, Object> code2Session(Long tenantId, String jsCode) throws Exception {
        WechatConfig config = config(tenantId);
        if (config == null || blank(config.getMpAppid()) || blank(config.getMpSecretEnc())) {
            throw new BizException("未配置小程序 AppID/AppSecret，无法微信登录");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + enc(config.getMpAppid())
                + "&secret=" + enc(cryptoService.decrypt(config.getMpSecretEnc()))
                + "&js_code=" + enc(jsCode) + "&grant_type=authorization_code";
        return getJson(url);
    }

    public String oauthUrl(Long tenantId, String redirectUri, String state) {
        WechatConfig config = config(tenantId);
        if (config == null || blank(config.getOaAppid())) {
            throw new BizException("未配置公众号 AppID，无法网页授权");
        }
        return "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + enc(config.getOaAppid())
                + "&redirect_uri=" + enc(redirectUri) + "&response_type=code&scope=snsapi_userinfo&state="
                + enc(state == null ? "portal" : state) + "#wechat_redirect";
    }

    public Map<String, Object> oauthUserInfo(Long tenantId, String code) throws Exception {
        WechatConfig config = config(tenantId);
        if (config == null || blank(config.getOaAppid()) || blank(config.getOaSecretEnc())) {
            throw new BizException("未配置公众号 AppID/AppSecret");
        }
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + enc(config.getOaAppid())
                + "&secret=" + enc(cryptoService.decrypt(config.getOaSecretEnc()))
                + "&code=" + enc(code) + "&grant_type=authorization_code";
        Map<String, Object> token = getJson(url);
        if (token.get("errcode") != null) throw new BizException("微信网页授权失败：" + token.get("errmsg"));
        String openid = String.valueOf(token.get("openid"));
        String access = String.valueOf(token.get("access_token"));
        String userUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=" + enc(access)
                + "&openid=" + enc(openid) + "&lang=zh_CN";
        Map<String, Object> user = getJson(userUrl);
        user.put("openid", openid);
        if (token.get("unionid") != null) user.put("unionid", token.get("unionid"));
        return user;
    }

    public Map<String, Object> sendTemplate(Long tenantId, String openid, String templateId,
                                            String url, Map<String, String> data) throws Exception {
        String token = officialAccessToken(tenantId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("touser", openid);
        payload.put("template_id", templateId);
        if (url != null && !url.isBlank()) payload.put("url", url);
        Map<String, Object> values = new LinkedHashMap<>();
        data.forEach((k, v) -> values.put(k, Map.of("value", v == null ? "" : v, "color", "#1f6feb")));
        payload.put("data", values);
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + token))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        Map<String, Object> result = objectMapper.readValue(http.send(request, HttpResponse.BodyHandlers.ofString()).body(),
                new TypeReference<>() {
                });
        if (result.get("errcode") != null && !"0".equals(String.valueOf(result.get("errcode")))) {
            throw new BizException("模板消息发送失败：" + result.get("errmsg"));
        }
        return result;
    }

    public Map<String, Object> createMenu(Long tenantId, Map<String, Object> menu) throws Exception {
        String token = officialAccessToken(tenantId);
        String body = objectMapper.writeValueAsString(menu);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.weixin.qq.com/cgi-bin/menu/create?access_token=" + token))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return objectMapper.readValue(http.send(request, HttpResponse.BodyHandlers.ofString()).body(), new TypeReference<>() {
        });
    }

    public Map<String, Object> massSend(Long tenantId, String targetType, Long tagId, List<String> openids,
                                        String contentType, String content) throws Exception {
        String token = officialAccessToken(tenantId);
        Map<String, Object> payload = new LinkedHashMap<>();
        if ("OPENIDS".equals(targetType)) {
            payload.put("touser", openids);
        } else {
            Map<String, Object> filter = new LinkedHashMap<>();
            if ("TAG".equals(targetType)) {
                filter.put("is_to_all", false);
                filter.put("tag_id", tagId);
            } else {
                filter.put("is_to_all", true);
            }
            payload.put("filter", filter);
        }
        payload.put(contentType.toLowerCase(), Map.of("content", content));
        String body = objectMapper.writeValueAsString(payload);
        String path = "OPENIDS".equals(targetType) ? "/cgi-bin/message/mass/send" : "/cgi-bin/message/mass/sendall";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.weixin.qq.com" + path + "?access_token=" + token))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return objectMapper.readValue(http.send(request, HttpResponse.BodyHandlers.ofString()).body(), new TypeReference<>() {
        });
    }

    public String jsapiSignature(String ticket, String nonce, String timestamp, String url) throws Exception {
        String raw = "jsapi_ticket=" + ticket + "&noncestr=" + nonce + "&timestamp=" + timestamp + "&url=" + url;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public Map<String, Object> jsapiConfig(Long tenantId, String url) throws Exception {
        WechatConfig config = config(tenantId);
        if (config == null || blank(config.getOaAppid())) throw new BizException("未配置公众号 AppID");
        String token = officialAccessToken(tenantId);
        String key = "ticket:" + tenantId;
        TokenCache cache = tokenCache.get(key);
        String ticket;
        if (cache != null && cache.expireAt > System.currentTimeMillis()) {
            ticket = cache.token;
        } else {
            Map<String, Object> result = getJson("https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token="
                    + token + "&type=jsapi");
            ticket = String.valueOf(result.get("ticket"));
            tokenCache.put(key, new TokenCache(ticket, System.currentTimeMillis() + 7000_000));
        }
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appId", config.getOaAppid());
        result.put("nonceStr", nonce);
        result.put("timestamp", timestamp);
        result.put("signature", jsapiSignature(ticket, nonce, timestamp, url));
        result.put("jsApiList", List.of("updateAppMessageShareData", "updateTimelineShareData", "chooseImage", "getLocation"));
        return result;
    }

    public String publicOauthTenantCode(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        return tenant == null ? null : tenant.getCode();
    }

    private String accessToken(String cacheKey, String appid, String secret, String grantType) throws Exception {
        TokenCache cache = tokenCache.get(cacheKey);
        if (cache != null && cache.expireAt > System.currentTimeMillis()) return cache.token;
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=" + grantType
                + "&appid=" + enc(appid) + "&secret=" + enc(secret);
        Map<String, Object> result = getJson(url);
        if (result.get("access_token") == null) throw new BizException("获取微信 access_token 失败：" + result.get("errmsg"));
        String token = String.valueOf(result.get("access_token"));
        tokenCache.put(cacheKey, new TokenCache(token, System.currentTimeMillis() + 7000_000));
        return token;
    }

    private Map<String, Object> getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
        String body = http.send(request, HttpResponse.BodyHandlers.ofString()).body();
        return objectMapper.readValue(body, new TypeReference<>() {
        });
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
