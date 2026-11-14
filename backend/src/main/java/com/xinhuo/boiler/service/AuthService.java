package com.xinhuo.boiler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.AuthUser;
import com.xinhuo.boiler.common.BizException;
import com.xinhuo.boiler.common.JwtService;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String DEFAULT_TENANT_CODE = "HLJ";
    private final TenantMapper tenantMapper;
    private final TenantUserMapper tenantUserMapper;
    private final RoleDefMapper roleDefMapper;
    private final CustomerMapper customerMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public Map<String, Object> loginAdmin(String username, String password) {
        Tenant tenant = defaultTenant();
        TenantUser tenantUser = tenantUserMapper.selectOne(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tenant.getId())
                .eq(TenantUser::getUsername, username)
                .eq(TenantUser::getRoleCode, "ADMIN").last("limit 1"));
        if (tenantUser == null) throw new BizException(401, "用户名或密码错误");
        checkPassword(password, tenantUser.getPassword());
        if (!"NORMAL".equals(tenantUser.getStatus())) throw new BizException(403, "账号已停用");
        return result(toAuthUser(tenant, tenantUser), tenant, "管理员");
    }

    public Map<String, Object> loginCustomer(String username, String password) {
        Tenant tenant = defaultTenant();
        TenantUser tenantUser = tenantUserMapper.selectOne(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tenant.getId())
                .eq(TenantUser::getUsername, username)
                .eq(TenantUser::getRoleCode, "CUSTOMER").last("limit 1"));
        if (tenantUser == null) throw new BizException(401, "账号或密码错误");
        checkPassword(password, tenantUser.getPassword());
        if (!"NORMAL".equals(tenantUser.getStatus())) throw new BizException(403, "账号已停用");
        AuthUser authUser = toAuthUser(tenant, tenantUser);
        if (authUser.getCustomerId() == null) throw new BizException(403, "用户未绑定客户档案");
        Customer customer = customerMapper.selectById(authUser.getCustomerId());
        if (customer == null) throw new BizException(403, "客户档案不存在");
        Map<String, Object> result = result(authUser, tenant, "客户");
        result.put("customer", customer);
        return result;
    }

    public Map<String, Object> loginUser(String username, String password) {
        Tenant tenant = defaultTenant();
        TenantUser tenantUser = tenantUserMapper.selectOne(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tenant.getId())
                .eq(TenantUser::getUsername, username).last("limit 1"));
        if (tenantUser == null) throw new BizException(401, "账号或密码错误");
        checkPassword(password, tenantUser.getPassword());
        if (!"NORMAL".equals(tenantUser.getStatus())) throw new BizException(403, "账号已停用");
        AuthUser authUser = toAuthUser(tenant, tenantUser);
        if ("CUSTOMER".equals(tenantUser.getRoleCode())) {
            if (authUser.getCustomerId() == null) throw new BizException(403, "用户未绑定客户档案");
            Customer customer = customerMapper.selectById(authUser.getCustomerId());
            if (customer == null) throw new BizException(403, "客户档案不存在");
            Map<String, Object> result = result(authUser, tenant, "客户");
            result.put("customer", customer);
            return result;
        }
        return result(authUser, tenant, roleName(tenantUser.getRoleCode()));
    }

    public Map<String, Object> buildSession(Tenant tenant, TenantUser tenantUser, Customer customer) {
        AuthUser authUser = toAuthUser(tenant, tenantUser);
        Map<String, Object> result = result(authUser, tenant,
                "CUSTOMER".equals(tenantUser.getRoleCode()) ? "客户" : roleName(tenantUser.getRoleCode()));
        if (customer != null) {
            result.put("customer", customer);
        }
        return result;
    }

    public Tenant requireCompany() {
        return defaultTenant();
    }

    private Tenant defaultTenant() {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, DEFAULT_TENANT_CODE).last("limit 1"));
        if (tenant == null) throw new BizException(404, "系统企业信息不存在");
        if ("DISABLED".equals(tenant.getStatus()) || "EXPIRED".equals(tenant.getStatus())) {
            throw new BizException(403, "系统已停用，请联系管理员");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(403, "系统已到期，请联系管理员");
        }
        return tenant;
    }

    private AuthUser toAuthUser(Tenant tenant, TenantUser tenantUser) {
        RoleDef role = roleDefMapper.selectOne(new LambdaQueryWrapper<RoleDef>()
                .eq(RoleDef::getCode, tenantUser.getRoleCode()).last("limit 1"));
        return AuthUser.builder()
                .id(tenantUser.getId()).username(tenantUser.getUsername()).name(tenantUser.getName())
                .userType("CUSTOMER".equals(tenantUser.getRoleCode()) ? "CUSTOMER" : "ADMIN")
                .tenantId(tenant.getId()).tenantCode(tenant.getCode()).tenantName(tenant.getName())
                .roleCode(tenantUser.getRoleCode()).customerId(tenantUser.getCustomerId())
                .permissions(role == null ? "" : role.getPermissions()).build();
    }

    private Map<String, Object> result(AuthUser user, Tenant tenant, String roleName) {
        String token = jwtService.create(user);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("token", token);
        map.put("user", user);
        map.put("company", company(tenant));
        map.put("roleName", roleName);
        map.put("permissions", user.getPermissions());
        return map;
    }

    public Map<String, Object> company(Tenant tenant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tenant.getId());
        map.put("name", tenant.getName());
        map.put("industry", tenant.getIndustry());
        map.put("contactName", tenant.getContactName());
        map.put("contactPhone", tenant.getContactPhone());
        map.put("maxUsers", tenant.getMaxUsers());
        map.put("maxCustomers", tenant.getMaxCustomers());
        return map;
    }

    private void checkPassword(String raw, String stored) {
        boolean matches;
        if (stored != null && stored.startsWith("plain:")) {
            matches = stored.substring(6).equals(raw);
        } else {
            matches = passwordEncoder.matches(raw, stored);
        }
        if (!matches) throw new BizException(401, "用户名或密码错误");
    }

    private String roleName(String code) {
        RoleDef role = roleDefMapper.selectOne(new LambdaQueryWrapper<RoleDef>()
                .eq(RoleDef::getCode, code).last("limit 1"));
        return role == null ? code : role.getName();
    }
}
