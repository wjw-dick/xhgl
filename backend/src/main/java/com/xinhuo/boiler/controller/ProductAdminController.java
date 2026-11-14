package com.xinhuo.boiler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.entity.Product;
import com.xinhuo.boiler.mapper.ProductMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin/products")
public class ProductAdminController extends SimpleCrudController<Product> {
    public ProductAdminController(ProductMapper mapper, AuditService auditService, ExcelService excelService,
                                  ObjectMapper objectMapper) {
        super(mapper, auditService, excelService, objectMapper, "锅炉产品",
                List.of("model", "code", "usage_desc"),
                List.of("boiler_type", "fuel_type", "pressure_level", "online_status", "compliance_status"),
                new LinkedHashMap<>() {{
                    put("code", "产品编码");
                    put("model", "产品型号");
                    put("boilerType", "锅炉类型");
                    put("fuelType", "燃料类型");
                    put("capacity", "蒸发量/热功率");
                    put("pressureLevel", "压力等级");
                    put("guidePrice", "指导价");
                    put("onlineStatus", "上架状态");
                    put("complianceStatus", "合规状态");
                }});
    }

    @PostMapping("/import")
    public ApiResult<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> rows = excelService.read(file);
        int success = 0;
        List<String> errors = new ArrayList<>();
        Map<String, String> boilerMap = Map.of("蒸汽锅炉", "steam", "热水锅炉", "hotwater", "导热油炉", "thermal", "余热锅炉", "wasteheat");
        Map<String, String> fuelMap = Map.of("燃气", "gas", "燃油", "oil", "电", "electric", "生物质", "biomass", "燃煤", "coal");
        Map<String, String> pressureMap = Map.of("常压", "atmospheric", "低压", "low", "中压", "medium", "高压", "high");
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String code = row.getOrDefault("产品编码", "").trim();
            String model = row.getOrDefault("产品型号", "").trim();
            if (code.isBlank() || model.isBlank()) {
                errors.add("第" + (i + 2) + "行：产品编码/型号为空");
                continue;
            }
            Product existing = mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                    .eq(Product::getCode, code).last("limit 1"));
            if (existing != null) {
                errors.add("第" + (i + 2) + "行：产品编码已存在 " + code);
                continue;
            }
            try {
                Product product = new Product();
                product.setTenantId(AuthContext.tenantId());
                product.setCode(code);
                product.setModel(model);
                product.setBoilerType(translate(row.getOrDefault("锅炉类型", "蒸汽锅炉"), boilerMap));
                product.setFuelType(translate(row.getOrDefault("燃料类型", "燃气"), fuelMap));
                product.setCapacity(row.getOrDefault("蒸发量/热功率", ""));
                product.setPressureLevel(translate(row.getOrDefault("压力等级", "低压"), pressureMap));
                product.setUsageDesc(row.getOrDefault("适用场景", ""));
                product.setTechParams(row.getOrDefault("技术参数", ""));
                product.setGuidePrice(new BigDecimal(row.getOrDefault("指导价（元）", row.getOrDefault("指导价", "0")).replace(",", "")));
                product.setOnlineStatus("已上架".equals(row.get("上架状态")) ? "ONLINE" : row.getOrDefault("上架状态", "ONLINE"));
                product.setComplianceStatus("缺少资质".equals(row.get("合规状态")) ? "MISSING" : "OK");
                product.setCertSummary(row.getOrDefault("资质摘要", ""));
                product.setRemark(row.getOrDefault("备注", ""));
                mapper.insert(product);
                success++;
            } catch (Exception e) {
                errors.add("第" + (i + 2) + "行：" + e.getMessage());
            }
        }
        auditService.log("锅炉产品", "批量导入", file.getOriginalFilename(), "成功" + success + "，失败" + errors.size());
        return ApiResult.ok(Map.of("success", success, "failed", errors.size(), "errors", errors));
    }

    private String translate(String value, Map<String, String> map) {
        return map.getOrDefault(value, value);
    }
}
