package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.BizException;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.PdfService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class TemplatePdfController {
    private final ContractMapper contractMapper;
    private final CustomerMapper customerMapper;
    private final InstallationMapper installationMapper;
    private final EquipmentMapper equipmentMapper;
    private final MaintenancePlanMapper maintenanceMapper;
    private final PdfService pdfService;
    private final AuditService auditService;

    @GetMapping("/contracts/{id}/acceptance-pdf")
    public void acceptancePdf(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) throw new BizException(404, "合同不存在");
        Customer customer = contract.getCustomerId() == null ? null : customerMapper.selectById(contract.getCustomerId());
        Equipment equipment = equipmentMapper.selectOne(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getContractId, id).last("limit 1"));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("合同编号", contract.getContractNo());
        fields.put("客户", contract.getCustomerName());
        fields.put("设备编号", equipment == null ? "待建档" : equipment.getEquipmentNo());
        fields.put("设备型号", equipment == null ? "—" : equipment.getModel());
        fields.put("安装地址", equipment == null ? "—" : equipment.getInstallAddr());
        fields.put("安装日期", equipment == null ? "—" : String.valueOf(equipment.getInstallDate()));
        fields.put("质保期至", equipment == null ? "—" : String.valueOf(equipment.getWarrantyEnd()));
        fields.put("客户联系人", customer == null ? "—" : customer.getName());
        List<String[]> table = new ArrayList<>();
        table.add(new String[]{"履约节点", "计划日期", "实际日期", "状态", "验收签字"});
        for (Installation installation : installationMapper.selectList(new LambdaQueryWrapper<Installation>()
                .eq(Installation::getContractId, id).orderByAsc(Installation::getId))) {
            table.add(new String[]{stage(installation.getStage()), String.valueOf(installation.getPlanDate()),
                    String.valueOf(installation.getActualDate()), installation.getStatus(),
                    installation.getAcceptanceSigner() == null ? "" : installation.getAcceptanceSigner()});
        }
        byte[] bytes = pdfService.build("锅炉安装验收单", "合同编号：" + contract.getContractNo(), fields, table,
                List.of("验收标准：锅炉本体、辅机、管道、电气与安全附件安装符合设计与 TSG 11 要求；监检资料齐全。",
                        "验收结论：□ 合格   □ 需整改（整改内容附后）。"),
                "客户验收签字：____________________    安装单位（盖章）：____________________    日期：__________");
        download(response, bytes, contract.getContractNo() + "-验收单.pdf");
        auditService.log("安装履约", "验收单PDF", contract.getContractNo(), null);
    }

    @GetMapping("/contracts/{id}/installation-pdf")
    public void installationPdf(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) throw new BizException(404, "合同不存在");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("合同编号", contract.getContractNo());
        fields.put("客户", contract.getCustomerName());
        fields.put("合同金额", String.valueOf(contract.getAmount()));
        List<String[]> table = new ArrayList<>();
        table.add(new String[]{"节点", "计划日期", "实际日期", "状态", "备注"});
        for (Installation installation : installationMapper.selectList(new LambdaQueryWrapper<Installation>()
                .eq(Installation::getContractId, id).orderByAsc(Installation::getId))) {
            table.add(new String[]{stage(installation.getStage()), String.valueOf(installation.getPlanDate()),
                    String.valueOf(installation.getActualDate()), installation.getStatus(), installation.getRemark()});
        }
        byte[] bytes = pdfService.build("安装调试记录", "合同编号：" + contract.getContractNo(), fields, table,
                List.of("安装单位须持有相应安装许可；安装过程执行安全技术规范并接受监督检验。"),
                "安装负责人：____________________    客户现场负责人：____________________    日期：__________");
        download(response, bytes, contract.getContractNo() + "-安装调试记录.pdf");
        auditService.log("安装履约", "安装记录PDF", contract.getContractNo(), null);
    }

    @GetMapping("/maintenance/{id}/pdf")
    public void maintenancePdf(@PathVariable Long id, HttpServletResponse response) throws Exception {
        MaintenancePlan plan = maintenanceMapper.selectById(id);
        if (plan == null) throw new BizException(404, "维保计划不存在");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("设备编号", plan.getEquipmentNo());
        fields.put("客户", plan.getCustomerName());
        fields.put("维保项目", plan.getProject());
        fields.put("周期（月）", String.valueOf(plan.getCycleMonths()));
        fields.put("上次完成", String.valueOf(plan.getLastDoneDate()));
        fields.put("下次到期", String.valueOf(plan.getNextDate()));
        fields.put("提醒天数", String.valueOf(plan.getRemindDays()));
        fields.put("备注", plan.getRemark());
        byte[] bytes = pdfService.build("锅炉维保报告", "设备：" + (plan.getEquipmentNo() == null ? "—" : plan.getEquipmentNo()),
                fields, null, List.of("检查项目：安全阀、水位计、压力表、排污阀、燃烧器、给水泵、控制系统。",
                        "结论：□ 正常   □ 需维修（维修内容附后）。维保记录按 TSG 11 要求归档。"),
                "维保技师：____________________    客户确认：____________________    日期：__________");
        download(response, bytes, (plan.getEquipmentNo() == null ? "维保" : plan.getEquipmentNo()) + "-维保报告.pdf");
        auditService.log("维保计划", "维保报告PDF", plan.getEquipmentNo(), null);
    }

    private String stage(String stage) {
        return switch (stage == null ? "" : stage) {
            case "SHIPPED" -> "发货";
            case "ARRIVED" -> "到货";
            case "INSTALL" -> "安装";
            case "ACCEPTANCE" -> "验收";
            default -> stage;
        };
    }

    private void download(HttpServletResponse response, byte[] bytes, String filename) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
        response.getOutputStream().write(bytes);
    }
}
