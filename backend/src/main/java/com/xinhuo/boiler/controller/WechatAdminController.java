package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.CryptoService;
import com.xinhuo.boiler.service.WechatGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/wechat")
@RequiredArgsConstructor
public class WechatAdminController {
    private final WechatConfigMapper configMapper;
    private final WechatMaterialMapper materialMapper;
    private final WechatTagMapper tagMapper;
    private final WechatFanMapper fanMapper;
    private final WechatMassLogMapper massLogMapper;
    private final CryptoService cryptoService;
    private final WechatGateway wechatGateway;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @GetMapping("/config")
    public ApiResult<Map<String, Object>> config() {
        var user = AuthContext.require();
        WechatConfig config = configMapper.selectOne(new LambdaQueryWrapper<WechatConfig>()
                .eq(WechatConfig::getTenantId, user.getTenantId()).last("limit 1"));
        Map<String, Object> result = new LinkedHashMap<>();
        if (config == null) config = new WechatConfig();
        result.put("mpAppid", config.getMpAppid());
        result.put("mpSecret", config.getMpSecretEnc() == null || config.getMpSecretEnc().isBlank() ? "" : "********");
        result.put("mpToken", config.getMpToken());
        result.put("mpAesKey", config.getMpAesKeyEnc() == null || config.getMpAesKeyEnc().isBlank() ? "" : "********");
        result.put("mpServerUrl", config.getMpServerUrl());
        result.put("mpRequestDomain", config.getMpRequestDomain());
        result.put("mpUploadDomain", config.getMpUploadDomain());
        result.put("mpDownloadDomain", config.getMpDownloadDomain());
        result.put("oaAppid", config.getOaAppid());
        result.put("oaSecret", config.getOaSecretEnc() == null || config.getOaSecretEnc().isBlank() ? "" : "********");
        result.put("oaServerUrl", config.getOaServerUrl());
        result.put("oaToken", config.getOaToken());
        result.put("oaAesKey", config.getOaAesKeyEnc() == null || config.getOaAesKeyEnc().isBlank() ? "" : "********");
        result.put("oauthDomain", config.getOauthDomain());
        result.put("jsDomain", config.getJsDomain());
        result.put("openPlatformAppid", config.getOpenPlatformAppid());
        result.put("welcomeText", config.getWelcomeText());
        result.put("keywordReply", config.getKeywordReply());
        result.put("menuJson", config.getMenuJson());
        return ApiResult.ok(result);
    }

    @PutMapping("/config")
    public ApiResult<Map<String, Object>> saveConfig(@RequestBody Map<String, String> body) {
        var user = AuthContext.require();
        WechatConfig config = configMapper.selectOne(new LambdaQueryWrapper<WechatConfig>()
                .eq(WechatConfig::getTenantId, user.getTenantId()).last("limit 1"));
        if (config == null) {
            config = new WechatConfig();
            config.setTenantId(user.getTenantId());
            configMapper.insert(config);
        }
        config.setMpAppid(body.getOrDefault("mpAppid", config.getMpAppid()));
        config.setMpToken(body.getOrDefault("mpToken", config.getMpToken()));
        config.setMpServerUrl(body.getOrDefault("mpServerUrl", config.getMpServerUrl()));
        config.setMpRequestDomain(body.getOrDefault("mpRequestDomain", config.getMpRequestDomain()));
        config.setMpUploadDomain(body.getOrDefault("mpUploadDomain", config.getMpUploadDomain()));
        config.setMpDownloadDomain(body.getOrDefault("mpDownloadDomain", config.getMpDownloadDomain()));
        config.setOaAppid(body.getOrDefault("oaAppid", config.getOaAppid()));
        config.setOaServerUrl(body.getOrDefault("oaServerUrl", config.getOaServerUrl()));
        config.setOaToken(body.getOrDefault("oaToken", config.getOaToken()));
        config.setOauthDomain(body.getOrDefault("oauthDomain", config.getOauthDomain()));
        config.setJsDomain(body.getOrDefault("jsDomain", config.getJsDomain()));
        config.setOpenPlatformAppid(body.getOrDefault("openPlatformAppid", config.getOpenPlatformAppid()));
        config.setWelcomeText(body.getOrDefault("welcomeText", config.getWelcomeText()));
        config.setKeywordReply(body.getOrDefault("keywordReply", config.getKeywordReply()));
        config.setMenuJson(body.getOrDefault("menuJson", config.getMenuJson()));
        if (body.containsKey("mpSecret") && !body.get("mpSecret").isBlank() && !body.get("mpSecret").contains("********")) {
            config.setMpSecretEnc(cryptoService.encrypt(body.get("mpSecret")));
        }
        if (body.containsKey("oaSecret") && !body.get("oaSecret").isBlank() && !body.get("oaSecret").contains("********")) {
            config.setOaSecretEnc(cryptoService.encrypt(body.get("oaSecret")));
        }
        if (body.containsKey("mpAesKey") && !body.get("mpAesKey").isBlank() && !body.get("mpAesKey").contains("********")) {
            config.setMpAesKeyEnc(cryptoService.encrypt(body.get("mpAesKey")));
        }
        if (body.containsKey("oaAesKey") && !body.get("oaAesKey").isBlank() && !body.get("oaAesKey").contains("********")) {
            config.setOaAesKeyEnc(cryptoService.encrypt(body.get("oaAesKey")));
        }
        configMapper.updateById(config);
        auditService.log("微信配置", "保存配置", null, "小程序/公众号/开放平台");
        return config();
    }

    @GetMapping("/menu")
    public ApiResult<Map<String, Object>> menu() {
        var user = AuthContext.require();
        WechatConfig config = wechatGateway.config(user.getTenantId());
        Map<String, Object> result = new LinkedHashMap<>();
        String menuJson = config == null ? null : config.getMenuJson();
        if (menuJson == null || menuJson.isBlank()) {
            Map<String, Object> urlButton = Map.of("type", "view", "name", "产品中心", "url", "https://portal.xinhuo.local/products");
            Map<String, Object> demandButton = Map.of("type", "view", "name", "提交需求", "url", "https://portal.xinhuo.local/demand");
            Map<String, Object> serviceButton = Map.of("type", "miniprogram", "name", "售后报修",
                    "url", "https://portal.xinhuo.local/service", "appid", "", "pagepath", "pages/service/repair");
            result.put("button", List.of(urlButton, demandButton, serviceButton));
        } else {
            try {
                result = objectMapper.readValue(menuJson, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception e) {
                result.put("button", List.of());
            }
        }
        return ApiResult.ok(result);
    }

    @PutMapping("/menu")
    public ApiResult<Map<String, Object>> saveMenu(@RequestBody Map<String, Object> menu) {
        var user = AuthContext.require();
        WechatConfig config = wechatGateway.config(user.getTenantId());
        if (config == null) throw new BizException("请先保存公众号配置");
        try {
            config.setMenuJson(objectMapper.writeValueAsString(menu));
        } catch (Exception e) {
            throw new BizException("菜单 JSON 格式错误");
        }
        configMapper.updateById(config);
        auditService.log("公众号菜单", "保存菜单", null, null);
        return ApiResult.ok(menu);
    }

    @PostMapping("/menu/sync")
    public ApiResult<Map<String, Object>> syncMenu() throws Exception {
        var user = AuthContext.require();
        Map<String, Object> menu = menu().getData();
        Map<String, Object> result;
        try {
            result = wechatGateway.createMenu(user.getTenantId(), menu);
        } catch (Exception e) {
            auditService.log("公众号菜单", "同步失败", null, e.getMessage());
            throw new BizException("菜单同步微信失败（需真实服务号配置）：" + e.getMessage());
        }
        auditService.log("公众号菜单", "同步微信", null, String.valueOf(result));
        return ApiResult.ok(result);
    }

    @GetMapping("/materials")
    public ApiResult<PageResult<WechatMaterial>> materials(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "10") long size) {
        Page<WechatMaterial> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        return ApiResult.ok(PageResult.of(materialMapper.selectPage(p, new LambdaQueryWrapper<WechatMaterial>()
                .orderByDesc(WechatMaterial::getId))));
    }

    @PostMapping("/materials")
    public ApiResult<WechatMaterial> addMaterial(@RequestBody WechatMaterial material) {
        var user = AuthContext.require();
        material.setId(null);
        material.setTenantId(user.getTenantId());
        if (material.getStatus() == null) material.setStatus("DRAFT");
        materialMapper.insert(material);
        auditService.log("公众号素材", "新增", material.getTitle(), null);
        return ApiResult.ok(material);
    }

    @DeleteMapping("/materials/{id}")
    public ApiResult<Void> deleteMaterial(@PathVariable Long id) {
        materialMapper.deleteById(id);
        auditService.log("公众号素材", "删除", String.valueOf(id), null);
        return ApiResult.ok();
    }

    @GetMapping("/tags")
    public ApiResult<List<WechatTag>> tags() {
        return ApiResult.ok(tagMapper.selectList(new LambdaQueryWrapper<WechatTag>().orderByAsc(WechatTag::getId)));
    }

    @PostMapping("/tags")
    public ApiResult<WechatTag> addTag(@RequestBody WechatTag tag) {
        var user = AuthContext.require();
        tag.setId(null);
        tag.setTenantId(user.getTenantId());
        tag.setFanCount(0);
        tagMapper.insert(tag);
        auditService.log("公众号粉丝标签", "新增", tag.getName(), null);
        return ApiResult.ok(tag);
    }

    @DeleteMapping("/tags/{id}")
    public ApiResult<Void> deleteTag(@PathVariable Long id) {
        tagMapper.deleteById(id);
        auditService.log("公众号粉丝标签", "删除", String.valueOf(id), null);
        return ApiResult.ok();
    }

    @GetMapping("/fans")
    public ApiResult<PageResult<WechatFan>> fans(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        Page<WechatFan> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        return ApiResult.ok(PageResult.of(fanMapper.selectPage(p, new LambdaQueryWrapper<WechatFan>()
                .orderByDesc(WechatFan::getId))));
    }

    @PostMapping("/fans/tag")
    public ApiResult<Map<String, Object>> tagFans(@RequestBody Map<String, Object> body) {
        List<Long> fanIds = ((List<?>) body.getOrDefault("fanIds", List.of())).stream()
                .map(v -> Long.valueOf(String.valueOf(v))).toList();
        String tags = String.valueOf(body.getOrDefault("tags", ""));
        for (Long fanId : fanIds) {
            WechatFan fan = fanMapper.selectById(fanId);
            if (fan != null) {
                fan.setTags(tags);
                fanMapper.updateById(fan);
            }
        }
        auditService.log("公众号粉丝标签", "批量打标签", null, "粉丝" + fanIds.size() + "个，标签" + tags);
        return ApiResult.ok(Map.of("updated", fanIds.size()));
    }

    @GetMapping("/mass-logs")
    public ApiResult<PageResult<WechatMassLog>> massLogs(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size) {
        Page<WechatMassLog> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        return ApiResult.ok(PageResult.of(massLogMapper.selectPage(p, new LambdaQueryWrapper<WechatMassLog>()
                .orderByDesc(WechatMassLog::getId))));
    }

    @PostMapping("/mass-send")
    public ApiResult<WechatMassLog> massSend(@RequestBody Map<String, Object> body) throws Exception {
        var user = AuthContext.require();
        WechatMassLog log = new WechatMassLog();
        log.setTenantId(user.getTenantId());
        log.setTargetType(String.valueOf(body.getOrDefault("targetType", "ALL")));
        log.setTagId(body.get("tagId") == null ? null : String.valueOf(body.get("tagId")));
        log.setOpenids(String.valueOf(body.getOrDefault("openids", "")));
        log.setContentType(String.valueOf(body.getOrDefault("contentType", "text")));
        log.setContent(String.valueOf(body.getOrDefault("content", "")));
        log.setStatus("QUEUED");
        massLogMapper.insert(log);
        try {
            List<String> openids = log.getOpenids().isBlank() ? List.of() : Arrays.asList(log.getOpenids().split(","));
            Map<String, Object> result = wechatGateway.massSend(user.getTenantId(), log.getTargetType(),
                    log.getTagId() == null ? null : Long.valueOf(log.getTagId()), openids, log.getContentType(), log.getContent());
            log.setStatus("SENT");
            log.setResult(String.valueOf(result));
        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setResult("群发失败（需认证服务号）：" + e.getMessage());
        }
        massLogMapper.updateById(log);
        auditService.log("公众号群发", "定向群发", null, log.getStatus());
        return ApiResult.ok(log);
    }
}
