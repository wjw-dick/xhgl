package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.Menu;
import com.xinhuo.boiler.mapper.MenuMapper;
import com.xinhuo.boiler.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
public class MenuAdminController {
    private final MenuMapper menuMapper;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<List<Menu>> list(@RequestParam(defaultValue = "ADMIN") String target) {
        return ApiResult.ok(menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getTarget, target).orderByAsc(Menu::getSort).orderByAsc(Menu::getId)));
    }

    @PostMapping
    public ApiResult<Menu> create(@RequestBody Menu menu) {
        var user = AuthContext.require();
        menu.setId(null);
        menu.setTenantId(user.getTenantId());
        if (menu.getVisible() == null) menu.setVisible(1);
        if (menu.getSort() == null) menu.setSort(99);
        if (menu.getMenuType() == null) menu.setMenuType("PAGE");
        if (menu.getParentId() == null) menu.setParentId(0L);
        menuMapper.insert(menu);
        auditService.log("菜单配置", "新增", menu.getTarget(), menu.getTitle());
        return ApiResult.ok(menu);
    }

    @PutMapping("/{id}")
    public ApiResult<Menu> update(@PathVariable Long id, @RequestBody Menu menu) {
        Menu existing = menuMapper.selectById(id);
        if (existing == null) throw new BizException(404, "菜单不存在");
        menu.setId(id);
        menu.setTenantId(existing.getTenantId());
        menu.setTarget(existing.getTarget());
        menuMapper.updateById(menu);
        auditService.log("菜单配置", "修改", existing.getTitle(), null);
        return ApiResult.ok(menuMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        long children = menuMapper.selectCount(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id));
        if (children > 0) throw new BizException("请先删除子菜单");
        menuMapper.deleteById(id);
        auditService.log("菜单配置", "删除", String.valueOf(id), null);
        return ApiResult.ok();
    }

    @PostMapping("/reorder")
    public ApiResult<List<Menu>> reorder(@RequestBody List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            if (item.get("id") == null) continue;
            Menu menu = menuMapper.selectById(Long.valueOf(String.valueOf(item.get("id"))));
            if (menu == null) continue;
            if (item.get("sort") != null) menu.setSort(Integer.valueOf(String.valueOf(item.get("sort"))));
            if (item.get("parentId") != null) menu.setParentId(Long.valueOf(String.valueOf(item.get("parentId"))));
            menuMapper.updateById(menu);
        }
        auditService.log("菜单配置", "拖拽排序", null, "共" + items.size() + "项");
        return ApiResult.ok(menuMapper.selectList(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort)));
    }
}
