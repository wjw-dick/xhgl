package com.xinhuo.boiler.controller;

import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.common.BizException;
import com.xinhuo.boiler.entity.FileRecord;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.RateLimiter;
import com.xinhuo.boiler.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/customer/files")
@RequiredArgsConstructor
public class CustomerFileController {
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif", "image/bmp", "image/heic", "image/heif");
    private final StorageService storageService;
    private final AuditService auditService;
    private final RateLimiter rateLimiter;

    @PostMapping("/upload")
    public ApiResult<FileRecord> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(required = false) String category) {
        rateLimiter.check("customer-file:" + AuthContext.userId(), 30, 60);
        if ("ticket".equalsIgnoreCase(category)) {
            String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            if (!IMAGE_TYPES.contains(type)) {
                throw new BizException("报修照片仅支持 JPG/PNG/WEBP/GIF/HEIC 图片");
            }
            if (file.getSize() > 10L * 1024 * 1024) {
                throw new BizException("单张报修照片不能超过 10MB");
            }
        }
        FileRecord record = storageService.upload(file, category == null ? "customer" : category);
        auditService.log("客户附件", "上传", record.getOriginalName(), record.getUrl());
        return ApiResult.ok(record);
    }
}
