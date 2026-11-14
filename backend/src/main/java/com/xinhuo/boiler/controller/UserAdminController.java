package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {
    private final TenantUserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final RoleDefMapper roleMapper;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<PageResult<TenantUser>> list(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String roleCode) {
        Page<TenantUser> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<TenantUser> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) qw.and(w -> w.like(TenantUser::getName, keyword)
                .or().like(TenantUser::getUsername, keyword).or().like(TenantUser::getPhone, keyword));
        if (roleCode != null && !roleCode.isBlank()) qw.eq(TenantUser::getRoleCode, roleCode);
        qw.orderByDesc(TenantUser::getId);
        return ApiResult.ok(PageResult.of(userMapper.selectPage(p, qw)));
    }

    @GetMapping("/roles")
    public ApiResult<List<RoleDef>> roles() {
        return ApiResult.ok(roleMapper.selectList(new LambdaQueryWrapper<RoleDef>()
                .eq(RoleDef::getScope, "COMPANY").orderByAsc(RoleDef::getSort)));
    }

    @PostMapping
    public ApiResult<TenantUser> create(@RequestBody TenantUser user) {
        var current = AuthContext.require();
        Tenant tenant = tenantMapper.selectById(current.getTenantId());
        long count = userMapper.selectCount(new LambdaQueryWrapper<>());
        if (tenant != null && tenant.getMaxUsers() != null && count >= tenant.getMaxUsers()) {
            throw new BizException("系统用户数已达配额上限 " + tenant.getMaxUsers());
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) throw new BizException("用户名不能为空");
        if (user.getPassword() == null || user.getPassword().isBlank()) user.setPassword("123456");
        user.setId(null);
        user.setTenantId(current.getTenantId());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("NORMAL");
        validateRole(user);
        userMapper.insert(user);
        auditService.log("用户管理", "新增用户", user.getUsername(), user.getRoleCode());
        return ApiResult.ok(user);
    }

    @PutMapping("/{id}")
    public ApiResult<TenantUser> update(@PathVariable Long id, @RequestBody TenantUser user) {
        TenantUser existing = userMapper.selectById(id);
        if (existing == null) throw new BizException(404, "用户不存在");
        existing.setName(user.getName() == null ? existing.getName() : user.getName());
        existing.setPhone(user.getPhone() == null ? existing.getPhone() : user.getPhone());
        existing.setEmail(user.getEmail() == null ? existing.getEmail() : user.getEmail());
        existing.setRoleCode(user.getRoleCode() == null ? existing.getRoleCode() : user.getRoleCode());
        existing.setCustomerId(user.getCustomerId() == null ? existing.getCustomerId() : user.getCustomerId());
        existing.setStatus(user.getStatus() == null ? existing.getStatus() : user.getStatus());
        existing.setRemark(user.getRemark() == null ? existing.getRemark() : user.getRemark());
        if (user.getPassword() != null && !user.getPassword().isBlank() && !user.getPassword().contains("********")) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        validateRole(existing);
        userMapper.updateById(existing);
        auditService.log("用户管理", "修改用户", existing.getUsername(), existing.getRoleCode());
        return ApiResult.ok(existing);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        var current = AuthContext.require();
        if (current.getId().equals(id)) throw new BizException("不能删除当前登录账号");
        TenantUser existing = userMapper.selectById(id);
        if (existing == null) throw new BizException(404, "用户不存在");
        if ("ADMIN".equals(existing.getRoleCode()) && userMapper.selectCount(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getRoleCode, "ADMIN")) <= 1) {
            throw new BizException("至少保留一个管理员");
        }
        userMapper.deleteById(id);
        auditService.log("用户管理", "删除用户", existing.getUsername(), null);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reset-password")
    public ApiResult<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        TenantUser user = userMapper.selectById(id);
        if (user == null) throw new BizException(404, "用户不存在");
        String password = body.getOrDefault("password", "123456");
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
        auditService.log("用户管理", "重置密码", user.getUsername(), null);
        return ApiResult.ok();
    }

    private void validateRole(TenantUser user) {
        RoleDef role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDef>()
                .eq(RoleDef::getCode, user.getRoleCode()).last("limit 1"));
        if (role == null || !"COMPANY".equals(role.getScope())) throw new BizException("非法角色");
        if ("CUSTOMER".equals(user.getRoleCode()) && user.getCustomerId() == null) {
            throw new BizException("客户角色必须绑定客户档案");
        }
        if (user.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(user.getCustomerId());
            if (customer == null) throw new BizException("绑定的客户档案不存在");
        }
    }
}
