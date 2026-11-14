package com.xinhuo.boiler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.entity.SparePart;
import com.xinhuo.boiler.mapper.SparePartMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/spares")
public class SpareAdminController extends SimpleCrudController<SparePart> {
    public SpareAdminController(SparePartMapper mapper, AuditService auditService,
                                ExcelService excelService, ObjectMapper objectMapper) {
        super(mapper, auditService, excelService, objectMapper, "备品备件",
                List.of("code", "name", "model", "supplier"),
                List.of("status"),
                new LinkedHashMap<>() {{
                    put("code", "备件编码");
                    put("name", "名称");
                    put("model", "型号");
                    put("spec", "规格");
                    put("supplier", "供应商");
                    put("unitPrice", "单价");
                    put("stock", "库存");
                    put("safetyStock", "安全库存");
                }});
    }

    @PostMapping("/import")
    public ApiResult<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> rows = excelService.read(file);
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String code = row.getOrDefault("备件编码", "").trim();
            String name = row.getOrDefault("名称", "").trim();
            if (code.isBlank() || name.isBlank()) {
                errors.add("第" + (i + 2) + "行：备件编码/名称为空");
                continue;
            }
            try {
                SparePart part = new SparePart();
                part.setTenantId(AuthContext.tenantId());
                part.setCode(code);
                part.setName(name);
                part.setModel(row.getOrDefault("型号", ""));
                part.setSpec(row.getOrDefault("规格", ""));
                part.setUnit(row.getOrDefault("单位", "个"));
                part.setSupplier(row.getOrDefault("供应商", ""));
                part.setUnitPrice(new BigDecimal(row.getOrDefault("单价", "0").replace(",", "")));
                part.setStock(Integer.parseInt(row.getOrDefault("库存", "0").replaceAll("\\D", "")));
                part.setSafetyStock(Integer.parseInt(row.getOrDefault("安全库存", "0").replaceAll("\\D", "")));
                part.setStatus("NORMAL");
                mapper.insert(part);
                success++;
            } catch (Exception e) {
                errors.add("第" + (i + 2) + "行：" + e.getMessage());
            }
        }
        auditService.log("备品备件", "批量导入", file.getOriginalFilename(), "成功" + success + "，失败" + errors.size());
        return ApiResult.ok(Map.of("success", success, "failed", errors.size(), "errors", errors));
    }
}
