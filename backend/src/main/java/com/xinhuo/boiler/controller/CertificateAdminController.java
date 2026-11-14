package com.xinhuo.boiler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.entity.Certificate;
import com.xinhuo.boiler.mapper.CertificateMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/certificates")
public class CertificateAdminController extends SimpleCrudController<Certificate> {
    public CertificateAdminController(CertificateMapper mapper, AuditService auditService,
                                      ExcelService excelService, ObjectMapper objectMapper) {
        super(mapper, auditService, excelService, objectMapper, "资质证书",
                List.of("cert_no", "cert_type", "issuer", "remark"),
                List.of("cert_type", "status", "product_id"),
                new LinkedHashMap<>() {{
                    put("certNo", "证书编号");
                    put("certType", "证书类型");
                    put("issuer", "发证机关");
                    put("validFrom", "有效期起");
                    put("validTo", "有效期止");
                    put("status", "状态");
                    put("remark", "备注");
                }});
    }

    @PostMapping("/import")
    public ApiResult<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> rows = excelService.read(file);
        int success = 0;
        List<String> errors = new ArrayList<>();
        Map<String, String> typeMap = Map.of("制造许可", "manufacture", "产品监检证明", "inspection", "能效测试报告", "energy",
                "安装许可", "install", "定期检验记录", "periodic");
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String certNo = row.getOrDefault("证书编号", "").trim();
            String certType = row.getOrDefault("证书类型", "").trim();
            if (certNo.isBlank() || certType.isBlank()) {
                errors.add("第" + (i + 2) + "行：证书编号/类型为空");
                continue;
            }
            try {
                Certificate cert = new Certificate();
                cert.setTenantId(AuthContext.tenantId());
                cert.setCertType(typeMap.getOrDefault(certType, certType));
                cert.setCertNo(certNo);
                cert.setIssuer(row.getOrDefault("发证机关", ""));
                String from = row.getOrDefault("有效期起", "");
                String to = row.getOrDefault("有效期止", "");
                if (!from.isBlank()) cert.setValidFrom(LocalDate.parse(from.length() > 10 ? from.substring(0, 10) : from));
                if (!to.isBlank()) cert.setValidTo(LocalDate.parse(to.length() > 10 ? to.substring(0, 10) : to));
                cert.setStatus("已过期".equals(row.get("状态")) ? "EXPIRED" : row.getOrDefault("状态", "VALID"));
                cert.setFileUrl(row.getOrDefault("扫描件", ""));
                cert.setRemark(row.getOrDefault("备注", ""));
                mapper.insert(cert);
                success++;
            } catch (Exception e) {
                errors.add("第" + (i + 2) + "行：" + e.getMessage());
            }
        }
        auditService.log("资质证书", "批量导入", file.getOriginalFilename(), "成功" + success + "，失败" + errors.size());
        return ApiResult.ok(Map.of("success", success, "failed", errors.size(), "errors", errors));
    }
}
