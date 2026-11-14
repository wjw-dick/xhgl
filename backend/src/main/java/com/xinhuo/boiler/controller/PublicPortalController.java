package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicPortalController {
    private final TenantMapper tenantMapper;
    private final TenantConfigMapper configMapper;
    private final ProductMapper productMapper;
    private final CaseLibraryMapper caseMapper;
    private final AnnouncementMapper announcementMapper;
    private final MenuMapper menuMapper;
    private final LanguageMapper languageMapper;
    private final CertificateMapper certificateMapper;
    private final RateLimiter rateLimiter;
    private final AuditService auditService;

    @GetMapping("/portal/home")
    public ApiResult<Map<String, Object>> home() {
        Tenant t = company();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("company", publicCompany(t));
        result.put("config", publicConfig(t.getId()));
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, t.getId()).eq(Product::getOnlineStatus, "ONLINE")
                .orderByAsc(Product::getId).last("limit 6"));
        result.put("products", products);
        result.put("cases", caseMapper.selectList(new LambdaQueryWrapper<CaseLibrary>()
                .eq(CaseLibrary::getTenantId, t.getId())
                .apply("UPPER(status) = 'PUBLISHED'")
                .orderByDesc(CaseLibrary::getId)));
        result.put("announcements", announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .and(w -> w.eq(Announcement::getTenantId, t.getId()).or().isNull(Announcement::getTenantId))
                .eq(Announcement::getStatus, "PUBLISHED").orderByDesc(Announcement::getPublishTime).last("limit 5")));
        result.put("menus", menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getTenantId, t.getId()).eq(Menu::getTarget, "H5").eq(Menu::getVisible, 1)
                .orderByAsc(Menu::getSort)));
        result.put("productCount", productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, t.getId()).eq(Product::getOnlineStatus, "ONLINE")));
        result.put("caseCount", caseMapper.selectCount(new LambdaQueryWrapper<CaseLibrary>()
                .eq(CaseLibrary::getTenantId, t.getId()).apply("UPPER(status) = 'PUBLISHED'")));
        return ApiResult.ok(result);
    }

    @GetMapping("/products")
    public ApiResult<PageResult<Product>> products(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "12") long size,
                                                   @RequestParam(required = false) String boilerType,
                                                   @RequestParam(required = false) String fuelType,
                                                   @RequestParam(required = false) String capacity,
                                                   @RequestParam(required = false) String keyword) {
        Tenant t = company();
        Page<Product> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 50));
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, t.getId()).eq(Product::getOnlineStatus, "ONLINE");
        if (boilerType != null && !boilerType.isBlank()) qw.eq(Product::getBoilerType, boilerType);
        if (fuelType != null && !fuelType.isBlank()) qw.eq(Product::getFuelType, fuelType);
        if (capacity != null && !capacity.isBlank()) qw.like(Product::getCapacity, capacity);
        if (keyword != null && !keyword.isBlank()) qw.and(w -> w.like(Product::getModel, keyword)
                .or().like(Product::getUsageDesc, keyword).or().like(Product::getCode, keyword));
        qw.orderByAsc(Product::getId);
        return ApiResult.ok(PageResult.of(productMapper.selectPage(p, qw)));
    }

    @GetMapping("/products/{id}")
    public ApiResult<Map<String, Object>> product(@PathVariable Long id) {
        Tenant t = company();
        Product product = productMapper.selectById(id);
        if (product == null || !product.getTenantId().equals(t.getId())) throw new BizException(404, "产品不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product", product);
        result.put("config", publicConfig(t.getId()));
        List<Map<String, Object>> certs = new ArrayList<>();
        for (Certificate cert : certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getTenantId, t.getId()).eq(Certificate::getProductId, id)
                .orderByAsc(Certificate::getValidTo))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("certType", cert.getCertType());
            item.put("certNo", maskCertNo(cert.getCertNo()));
            item.put("issuer", cert.getIssuer());
            item.put("validTo", cert.getValidTo());
            item.put("status", effectiveCertStatus(cert));
            certs.add(item);
        }
        result.put("certificates", certs);
        result.put("similar", productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, t.getId()).eq(Product::getBoilerType, product.getBoilerType())
                .eq(Product::getOnlineStatus, "ONLINE").ne(Product::getId, id).last("limit 3")));
        return ApiResult.ok(result);
    }

    @GetMapping("/cases")
    public ApiResult<List<CaseLibrary>> cases(@RequestParam(required = false) String industry) {
        Tenant t = company();
        LambdaQueryWrapper<CaseLibrary> qw = new LambdaQueryWrapper<CaseLibrary>()
                .eq(CaseLibrary::getTenantId, t.getId()).apply("UPPER(status) = 'PUBLISHED'");
        if (industry != null && !industry.isBlank()) qw.eq(CaseLibrary::getIndustry, industry);
        qw.orderByDesc(CaseLibrary::getId);
        return ApiResult.ok(caseMapper.selectList(qw));
    }

    @GetMapping("/announcements")
    public ApiResult<List<Announcement>> announcements() {
        Tenant t = company();
        return ApiResult.ok(announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .and(w -> w.eq(Announcement::getTenantId, t.getId()).or().isNull(Announcement::getTenantId))
                .eq(Announcement::getStatus, "PUBLISHED").orderByDesc(Announcement::getPublishTime).last("limit 50")));
    }

    @GetMapping("/announcements/{id}")
    public ApiResult<Announcement> announcement(@PathVariable Long id) {
        Tenant t = company();
        Announcement a = announcementMapper.selectById(id);
        if (a == null || (a.getTenantId() != null && !a.getTenantId().equals(t.getId()))) {
            throw new BizException(404, "公告不存在");
        }
        return ApiResult.ok(a);
    }

    @GetMapping("/menus")
    public ApiResult<List<Menu>> menus(@RequestParam(defaultValue = "H5") String target) {
        Tenant t = company();
        return ApiResult.ok(menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getTenantId, t.getId()).eq(Menu::getTarget, target).eq(Menu::getVisible, 1)
                .orderByAsc(Menu::getSort).orderByAsc(Menu::getId)));
    }

    @GetMapping("/languages")
    public ApiResult<List<Language>> languages() {
        Tenant t = company();
        return ApiResult.ok(languageMapper.selectList(new LambdaQueryWrapper<Language>()
                .eq(Language::getTenantId, t.getId()).eq(Language::getEnabled, 1).orderByAsc(Language::getSort)));
    }

    @GetMapping("/institution")
    public ApiResult<Map<String, Object>> institution() {
        Tenant t = company();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("company", publicCompany(t));
        result.put("config", publicConfig(t.getId()));
        result.put("products", productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, t.getId()).eq(Product::getOnlineStatus, "ONLINE").last("limit 8")));
        result.put("cases", caseMapper.selectList(new LambdaQueryWrapper<CaseLibrary>()
                .eq(CaseLibrary::getTenantId, t.getId()).apply("UPPER(status) = 'PUBLISHED'")));
        result.put("certificates", certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getTenantId, t.getId()).isNull(Certificate::getCustomerId)
                .in(Certificate::getCertType, "manufacture", "install", "energy").last("limit 6"))
                .stream().map(c -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("certType", c.getCertType());
                    item.put("certNo", maskCertNo(c.getCertNo()));
                    item.put("issuer", c.getIssuer());
                    item.put("validTo", String.valueOf(c.getValidTo()));
                    item.put("status", effectiveCertStatus(c));
                    return item;
                }).toList());
        return ApiResult.ok(result);
    }

    @GetMapping("/verify")
    public ApiResult<Map<String, Object>> verify(@RequestParam String certNo,
                                                 @RequestParam String keyword,
                                                 HttpServletRequest request) {
        if (keyword == null || keyword.trim().length() < 2) throw new BizException("请输入核验关键字");
        String ip = request.getRemoteAddr();
        rateLimiter.check("verify:" + ip, 60, 60);
        List<Certificate> certs = certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getCertNo, certNo).last("limit 1"));
        if (certs.isEmpty()) {
            Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                    .eq(Product::getCode, certNo).last("limit 1"));
            if (product != null) {
                certs = certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getTenantId, product.getTenantId())
                        .eq(Certificate::getProductId, product.getId()).orderByAsc(Certificate::getValidTo));
            }
        }
        if (certs.isEmpty()) throw new BizException(404, "未查询到对应资质，请核对编号");
        Certificate cert = certs.get(0);
        Product product = cert.getProductId() == null ? null : productMapper.selectById(cert.getProductId());
        Tenant tenant = tenantMapper.selectById(cert.getTenantId());
        String status = effectiveCertStatus(cert);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", "VALID".equals(status) || "WARNING".equals(status));
        result.put("status", status);
        result.put("certType", cert.getCertType());
        result.put("certNo", maskCertNo(cert.getCertNo()));
        result.put("issuer", cert.getIssuer());
        result.put("validTo", cert.getValidTo());
        result.put("productModel", product == null ? null : product.getModel());
        result.put("productCapacity", product == null ? null : product.getCapacity());
        result.put("enterprise", tenant == null ? null : tenant.getName());
        result.put("keywordAccepted", true);
        result.put("notice", "仅公示资质真伪、状态与有效期摘要，不返回客户及合同信息。");
        auditService.logForTenant(cert.getTenantId(), "资质核验", "公开核验", "访客",
                maskCertNo(cert.getCertNo()), "IP=" + ip);
        return ApiResult.ok(result);
    }

    public Tenant company() {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, "HLJ").last("limit 1"));
        if (tenant == null) throw new BizException(404, "企业信息不存在");
        if ("DISABLED".equals(tenant.getStatus()) || "EXPIRED".equals(tenant.getStatus())) {
            throw new BizException(403, "系统已停用");
        }
        return tenant;
    }

    private Map<String, Object> publicCompany(Tenant tenant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tenant.getId());
        map.put("name", tenant.getName());
        map.put("industry", tenant.getIndustry());
        map.put("contactName", tenant.getContactName());
        map.put("contactPhone", tenant.getContactPhone());
        return map;
    }

    private Map<String, String> publicConfig(Long tenantId) {
        Map<String, String> result = new LinkedHashMap<>();
        for (TenantConfig config : configMapper.selectList(new LambdaQueryWrapper<TenantConfig>()
                .eq(TenantConfig::getTenantId, tenantId))) {
            if (config.getEncrypted() != null && config.getEncrypted() == 1) continue;
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    private String maskCertNo(String certNo) {
        if (certNo == null || certNo.length() <= 8) return "****";
        return certNo.substring(0, Math.min(6, certNo.length() - 4)) + "-****-" + certNo.substring(certNo.length() - 4);
    }

    private String effectiveCertStatus(Certificate cert) {
        if (cert.getValidTo() == null) return "UNKNOWN";
        if (cert.getValidTo().isBefore(LocalDate.now())) return "EXPIRED";
        if (ChronoUnit.DAYS.between(LocalDate.now(), cert.getValidTo()) <= 90) return "WARNING";
        return "VALID";
    }
}
