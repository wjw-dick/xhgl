package com.xinhuo.boiler.controller;

import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.entity.FileRecord;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
public class FileController {
    private final StorageService storageService;
    private final AuditService auditService;

    @PostMapping("/upload")
    public ApiResult<FileRecord> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(required = false) String category) {
        FileRecord record = storageService.upload(file, category);
        auditService.log("附件", "上传", record.getOriginalName(), record.getUrl());
        return ApiResult.ok(record);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        storageService.deleteFile(id);
        auditService.log("附件", "删除", String.valueOf(id), null);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/url")
    public ApiResult<Map<String, String>> url(@PathVariable Long id) {
        FileRecord record = storageService.currentUpload(id);
        return ApiResult.ok(Map.of("url", storageService.signedUrl(record)));
    }
}
