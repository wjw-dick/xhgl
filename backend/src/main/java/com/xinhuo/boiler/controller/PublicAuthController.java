package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicAuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final TenantMapper tenantMapper;
    private final TenantUserMapper tenantUserMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final IdGenerator idGenerator;
    private final WechatGateway wechatGateway;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final AuditService auditService;

    @PostMapping("/auth/login")
    public ApiResult<Map<String, Object>> login(@RequestBody CustomerAuthController.CustomerLogin request,
                                                HttpServletRequest http) {
        rateLimiter.check("public-login:" + http.getRemoteAddr(), 10, 60);
        return ApiResult.ok(authService.loginCustomer(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/auth/user-login")
    public ApiResult<Map<String, Object>> userLogin(@RequestBody CustomerAuthController.CustomerLogin request,
                                                   HttpServletRequest http) {
        rateLimiter.check("public-login:" + http.getRemoteAddr(), 10, 60);
        return ApiResult.ok(authService.loginUser(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/auth/wechat")
    public ApiResult<Map<String, Object>> wechatLogin(@RequestBody Map<String, String> body,
                                                      HttpServletRequest request) throws Exception {
        rateLimiter.check("wechat-login:" + request.getRemoteAddr(), 10, 60);
        Tenant tenant = company();
        Map<String, Object> session = wechatGateway.code2Session(tenant.getId(), body.get("code"));
        String openid = String.valueOf(session.get("openid"));
        String unionid = session.get("unionid") == null ? "" : String.valueOf(session.get("unionid"));
        return ApiResult.ok(loginOrCreate(tenant, openid, unionid, body.getOrDefault("nickname", "")));
    }

    @PostMapping("/auth/wechat-oauth")
    public ApiResult<Map<String, Object>> wechatOauth(@RequestBody Map<String, String> body,
                                                      HttpServletRequest request) throws Exception {
        rateLimiter.check("wechat-oauth:" + request.getRemoteAddr(), 10, 60);
        Tenant tenant = company();
        Map<String, Object> user = wechatGateway.oauthUserInfo(tenant.getId(), body.get("code"));
        String openid = String.valueOf(user.get("openid"));
        String unionid = user.get("unionid") == null ? "" : String.valueOf(user.get("unionid"));
        return ApiResult.ok(loginOrCreate(tenant, openid, unionid,
                String.valueOf(user.getOrDefault("nickname", ""))));
    }

    @GetMapping("/oauth/wechat/url")
    public ApiResult<Map<String, Object>> oauthUrl(@RequestParam String redirect) {
        Tenant tenant = company();
        validateRedirect(tenant, redirect);
        String url = wechatGateway.oauthUrl(tenant.getId(), redirect, "portal");
        return ApiResult.ok(Map.of("url", url, "scope", "snsapi_userinfo"));
    }

    @GetMapping("/wechat/js-config")
    public ApiResult<Map<String, Object>> jsConfig(@RequestParam String url) throws Exception {
        Tenant tenant = company();
        return ApiResult.ok(wechatGateway.jsapiConfig(tenant.getId(), url));
    }

    private Map<String, Object> loginOrCreate(Tenant tenant, String openid, String unionid, String nickname) {
        Contact contact = null;
        if (unionid != null && !unionid.isBlank()) {
            contact = contactMapper.selectOne(new LambdaQueryWrapper<Contact>()
                    .eq(Contact::getTenantId, tenant.getId()).eq(Contact::getUnionid, unionid).last("limit 1"));
        }
        if (contact == null) {
            contact = contactMapper.selectOne(new LambdaQueryWrapper<Contact>()
                    .eq(Contact::getTenantId, tenant.getId()).eq(Contact::getWechatOpenid, openid).last("limit 1"));
        }
        Customer customer;
        if (contact != null) {
            customer = customerMapper.selectById(contact.getCustomerId());
            contact.setWechatOpenid(openid);
            if (unionid != null && !unionid.isBlank()) contact.setUnionid(unionid);
            contactMapper.updateById(contact);
        } else {
            customer = new Customer();
            customer.setTenantId(tenant.getId());
            customer.setCustomerNo(idGenerator.next(tenant.getId(), tenant.getCode(), "CUSTOMER"));
            customer.setName(nickname == null || nickname.isBlank()
                    ? "微信客户" + openid.substring(Math.max(0, openid.length() - 4)) : nickname);
            customer.setSource("WECHAT");
            customer.setCategory("线上客户");
            customer.setStatus("NORMAL");
            customerMapper.insert(customer);
            contact = new Contact();
            contact.setTenantId(tenant.getId());
            contact.setCustomerId(customer.getId());
            contact.setName(customer.getName());
            contact.setWechatOpenid(openid);
            contact.setUnionid(unionid);
            contact.setPrimaryFlag(1);
            contactMapper.insert(contact);
        }
        TenantUser user = tenantUserMapper.selectOne(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tenant.getId()).eq(TenantUser::getCustomerId, customer.getId())
                .eq(TenantUser::getRoleCode, "CUSTOMER").last("limit 1"));
        if (user == null) {
            user = new TenantUser();
            user.setTenantId(tenant.getId());
            user.setUsername("wx_" + openid.substring(Math.max(0, openid.length() - 10)));
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setName(customer.getName());
            user.setRoleCode("CUSTOMER");
            user.setCustomerId(customer.getId());
            user.setStatus("NORMAL");
            tenantUserMapper.insert(user);
        }
        AuthUser authUser = AuthUser.builder()
                .id(user.getId()).username(user.getUsername()).name(user.getName())
                .userType("CUSTOMER").tenantId(tenant.getId()).tenantCode(tenant.getCode())
                .tenantName(tenant.getName()).roleCode("CUSTOMER").customerId(customer.getId())
                .permissions("self").build();
        String token = jwtService.create(authUser);
        auditService.logForTenant(tenant.getId(), "微信登录", "UnionID绑定", user.getUsername(),
                openid, "unionid=" + unionid);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", authUser);
        result.put("customer", customer);
        result.put("company", authService.company(tenant));
        result.put("roleName", "客户");
        return result;
    }

    private Tenant company() {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, "HLJ").last("limit 1"));
        if (tenant == null) throw new BizException(404, "企业信息不存在");
        if ("DISABLED".equals(tenant.getStatus()) || "EXPIRED".equals(tenant.getStatus())) throw new BizException(403, "系统已停用");
        return tenant;
    }

    private void validateRedirect(Tenant tenant, String redirect) {
        if (redirect == null || redirect.isBlank()) throw new BizException("缺少回跳地址");
        if (redirect.startsWith("/")) return;
        try {
            URI uri = URI.create(redirect);
            String host = uri.getHost();
            if (host == null) throw new BizException("非法回跳地址");
            WechatConfig config = wechatGateway.config(tenant.getId());
            String allowed = config == null ? "" : config.getOauthDomain();
            if (host.equals("localhost") || host.equals("127.0.0.1")
                    || (allowed != null && !allowed.isBlank() && allowed.contains(host))) return;
            throw new BizException(403, "回跳域名不在公众号网页授权域名白名单内");
        } catch (IllegalArgumentException e) {
            throw new BizException("非法回跳地址");
        }
    }
}
