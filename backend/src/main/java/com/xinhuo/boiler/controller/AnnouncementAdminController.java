package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.Announcement;
import com.xinhuo.boiler.mapper.AnnouncementMapper;
import com.xinhuo.boiler.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AnnouncementAdminController {
    private final AnnouncementMapper announcementMapper;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<PageResult<Announcement>> list(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword) {
        var user = AuthContext.require();
        Page<Announcement> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) qw.like(Announcement::getTitle, keyword);
        qw.and(w -> w.eq(Announcement::getTenantId, user.getTenantId()).or().isNull(Announcement::getTenantId));
        qw.orderByDesc(Announcement::getId);
        return ApiResult.ok(PageResult.of(announcementMapper.selectPage(p, qw)));
    }

    @PostMapping
    public ApiResult<Announcement> create(@RequestBody Announcement announcement) {
        var user = AuthContext.require();
        announcement.setId(null);
        announcement.setTenantId(user.getTenantId());
        announcement.setSource("COMPANY");
        announcement.setStatus("PUBLISHED");
        if (announcement.getPublishTime() == null) announcement.setPublishTime(LocalDateTime.now());
        announcementMapper.insert(announcement);
        auditService.log("公告", "发布", String.valueOf(announcement.getId()), announcement.getTitle());
        return ApiResult.ok(announcement);
    }

    @PutMapping("/{id}")
    public ApiResult<Announcement> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        var user = AuthContext.require();
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) throw new BizException(404, "公告不存在");
        if (existing.getTenantId() == null || !existing.getTenantId().equals(user.getTenantId())) {
            throw new BizException(403, "系统公告不可修改");
        }
        existing.setTitle(announcement.getTitle());
        existing.setContent(announcement.getContent());
        existing.setStatus(announcement.getStatus() == null ? existing.getStatus() : announcement.getStatus());
        announcementMapper.updateById(existing);
        auditService.log("公告", "修改", String.valueOf(id), existing.getTitle());
        return ApiResult.ok(existing);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        var user = AuthContext.require();
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) throw new BizException(404, "公告不存在");
        if (existing.getTenantId() == null || !existing.getTenantId().equals(user.getTenantId())) {
            throw new BizException(403, "系统公告不可删除");
        }
        announcementMapper.deleteById(id);
        auditService.log("公告", "删除", String.valueOf(id), null);
        return ApiResult.ok();
    }
}
