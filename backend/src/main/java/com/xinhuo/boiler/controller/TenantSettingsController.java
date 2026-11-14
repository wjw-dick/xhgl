package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.CryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class TenantSettingsController {
    private final TenantConfigMapper configMapper;
    private final SysDictGlobalMapper globalDictMapper;
    private final SysDictTenantMapper tenantDictMapper;
    private final LanguageMapper languageMapper;
    private final TenantMapper tenantMapper;
    private final CryptoService cryptoService;
    private final AuditService auditService;

    @GetMapping("/configs")
    public ApiResult<List<TenantConfig>> configs() {
        List<TenantConfig> configs = configMapper.selectList(new LambdaQueryWrapper<TenantConfig>()
                .orderByAsc(TenantConfig::getId));
        configs.forEach(c -> {
            if (c.getEncrypted() != null && c.getEncrypted() == 1 && c.getConfigValue() != null
                    && !c.getConfigValue().isBlank()) {
                c.setConfigValue("********");
            }
        });
        return ApiResult.ok(configs);
    }

    @PutMapping("/configs")
    public ApiResult<List<TenantConfig>> saveConfigs(@RequestBody Map<String, String> body) {
        var user = AuthContext.require();
        for (Map.Entry<String, String> entry : body.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            TenantConfig existing = configMapper.selectOne(new LambdaQueryWrapper<TenantConfig>()
                    .eq(TenantConfig::getConfigKey, key).last("limit 1"));
            boolean secret = key.contains("secret") || key.contains("password") || key.contains("key");
            if (existing == null) {
                TenantConfig config = new TenantConfig();
                config.setTenantId(user.getTenantId());
                config.setConfigKey(key);
                config.setConfigValue(secret ? cryptoService.encrypt(value) : value);
                config.setEncrypted(secret ? 1 : 0);
                config.setGroupName("BASIC");
                configMapper.insert(config);
            } else {
                if (secret && (value == null || value.isBlank() || value.contains("********"))) {
                    continue;
                }
                existing.setConfigValue(secret ? cryptoService.encrypt(value) : value);
                existing.setEncrypted(secret ? 1 : 0);
                configMapper.updateById(existing);
            }
        }
        auditService.log("系统设置", "保存企业配置", null, body.keySet().toString());
        return configs();
    }

    @PutMapping("/profile")
    public ApiResult<Map<String, Object>> updateProfile(@RequestBody Map<String, String> body) {
        var user = AuthContext.require();
        Tenant tenant = tenantMapper.selectById(user.getTenantId());
        if (tenant == null) throw new BizException(404, "企业信息不存在");
        if (body.get("name") != null && !body.get("name").isBlank()) tenant.setName(body.get("name"));
        if (body.get("industry") != null) tenant.setIndustry(body.get("industry"));
        if (body.get("contactName") != null) tenant.setContactName(body.get("contactName"));
        if (body.get("contactPhone") != null) tenant.setContactPhone(body.get("contactPhone"));
        tenantMapper.updateById(tenant);
        auditService.log("系统设置", "更新企业资料", tenant.getName(), tenant.getIndustry());
        Map<String, Object> company = new LinkedHashMap<>();
        company.put("id", tenant.getId());
        company.put("name", tenant.getName());
        company.put("industry", tenant.getIndustry());
        company.put("contactName", tenant.getContactName());
        company.put("contactPhone", tenant.getContactPhone());
        company.put("maxUsers", tenant.getMaxUsers());
        company.put("maxCustomers", tenant.getMaxCustomers());
        return ApiResult.ok(company);
    }

    @GetMapping("/dicts")
    public ApiResult<Map<String, Object>> dicts() {
        var user = AuthContext.require();
        List<SysDictGlobal> global = globalDictMapper.selectList(new LambdaQueryWrapper<SysDictGlobal>()
                .eq(SysDictGlobal::getStatus, "NORMAL").orderByAsc(SysDictGlobal::getSort));
        List<SysDictTenant> custom = tenantDictMapper.selectList(new LambdaQueryWrapper<SysDictTenant>()
                .eq(SysDictTenant::getStatus, "NORMAL").orderByAsc(SysDictTenant::getSort));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("global", global);
        result.put("custom", custom);
        result.put("canEditGlobal", false);
        return ApiResult.ok(result);
    }

    @PostMapping("/dicts")
    public ApiResult<SysDictTenant> addDict(@RequestBody SysDictTenant dict) {
        var user = AuthContext.require();
        SysDictTenant existing = tenantDictMapper.selectOne(new LambdaQueryWrapper<SysDictTenant>()
                .eq(SysDictTenant::getDictType, dict.getDictType())
                .eq(SysDictTenant::getDictKey, dict.getDictKey()).last("limit 1"));
        if (existing != null) throw new BizException("自定义字典项已存在");
        dict.setId(null);
        dict.setTenantId(user.getTenantId());
        dict.setStatus("NORMAL");
        tenantDictMapper.insert(dict);
        auditService.log("数据字典", "新增自定义项", dict.getDictType(), dict.getDictValue());
        return ApiResult.ok(dict);
    }

    @DeleteMapping("/dicts/{id}")
    public ApiResult<Void> deleteDict(@PathVariable Long id) {
        tenantDictMapper.deleteById(id);
        auditService.log("数据字典", "删除自定义项", String.valueOf(id), null);
        return ApiResult.ok();
    }

    @GetMapping("/languages")
    public ApiResult<List<Language>> languages() {
        return ApiResult.ok(languageMapper.selectList(new LambdaQueryWrapper<Language>()
                .orderByAsc(Language::getSort)));
    }

    @PostMapping("/languages")
    public ApiResult<Language> addLanguage(@RequestBody Language language) {
        var user = AuthContext.require();
        language.setId(null);
        language.setTenantId(user.getTenantId());
        if (language.getEnabled() == null) language.setEnabled(1);
        languageMapper.insert(language);
        auditService.log("多语言", "新增语言", language.getCode(), language.getName());
        return ApiResult.ok(language);
    }

    @PutMapping("/languages/{id}")
    public ApiResult<Language> updateLanguage(@PathVariable Long id, @RequestBody Language language) {
        Language existing = languageMapper.selectById(id);
        if (existing == null) throw new BizException(404, "语言不存在");
        existing.setName(language.getName() == null ? existing.getName() : language.getName());
        existing.setEnabled(language.getEnabled() == null ? existing.getEnabled() : language.getEnabled());
        existing.setSort(language.getSort() == null ? existing.getSort() : language.getSort());
        languageMapper.updateById(existing);
        auditService.log("多语言", "更新语言", existing.getCode(), String.valueOf(existing.getEnabled()));
        return ApiResult.ok(existing);
    }
}
