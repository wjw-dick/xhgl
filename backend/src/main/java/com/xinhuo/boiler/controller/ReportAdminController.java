package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportAdminController {
    private final ContractMapper contractMapper;
    private final DemandMapper demandMapper;
    private final PaymentPlanMapper paymentMapper;
    private final ServiceTicketMapper ticketMapper;
    private final SpareRecordMapper spareRecordMapper;
    private final TenantUserMapper userMapper;
    private final ExcelService excelService;
    private final AuditService auditService;

    @GetMapping
    public ApiResult<Map<String, Object>> report() {
        LambdaQueryWrapper<Contract> contractQw = new LambdaQueryWrapper<>();
        List<Contract> contracts = contractMapper.selectList(contractQw.orderByDesc(Contract::getId));
        List<Long> contractIds = contracts.stream().map(Contract::getId).toList();
        List<PaymentPlan> plans = contractIds.isEmpty() ? List.of() : paymentMapper.selectList(
                new LambdaQueryWrapper<PaymentPlan>().in(PaymentPlan::getContractId, contractIds));
        BigDecimal amount = contracts.stream().map(c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = plans.stream().map(p -> p.getReceivedAmount() == null ? BigDecimal.ZERO : p.getReceivedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = plans.stream().filter(p -> !"PAID".equals(p.getStatus()))
                .map(p -> (p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                        .subtract(p.getReceivedAmount() == null ? BigDecimal.ZERO : p.getReceivedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LambdaQueryWrapper<Demand> demandQw = new LambdaQueryWrapper<>();
        List<Demand> demands = demandMapper.selectList(demandQw);
        Map<String, Long> funnel = new LinkedHashMap<>();
        for (String status : List.of("NEW", "CONSULTING", "INTRODUCING", "TALKING", "DEAL", "LOST")) {
            funnel.put(status, demandMapper.selectCount(demandQw.clone().eq(Demand::getStatus, status)));
        }
        long deals = funnel.getOrDefault("DEAL", 0L);
        List<Map<String, Object>> monthly = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<PaymentPlan> receivedPlans = plans;
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            BigDecimal monthAmount = contracts.stream().filter(c -> c.getCreatedAt() != null
                            && YearMonth.from(c.getCreatedAt()).equals(month))
                    .map(c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal monthReceived = receivedPlans.stream().filter(p -> p.getReceivedDate() != null
                            && YearMonth.from(p.getReceivedDate()).equals(month))
                    .map(p -> p.getReceivedAmount() == null ? BigDecimal.ZERO : p.getReceivedAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", month.format(fmt));
            item.put("contractAmount", monthAmount);
            item.put("received", monthReceived);
            item.put("contracts", contracts.stream().filter(c -> c.getCreatedAt() != null
                    && YearMonth.from(c.getCreatedAt()).equals(month)).count());
            monthly.add(item);
        }
        Map<Long, Map<String, Object>> rankingMap = new LinkedHashMap<>();
        for (Contract contract : contracts) {
            Long ownerId = contract.getOwnerId() == null ? 0L : contract.getOwnerId();
            Map<String, Object> row = rankingMap.computeIfAbsent(ownerId, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                TenantUser u = k == 0 ? null : userMapper.selectById(k);
                m.put("userId", k);
                m.put("name", u == null ? "未分配" : u.getName());
                m.put("contracts", 0);
                m.put("amount", BigDecimal.ZERO);
                m.put("received", BigDecimal.ZERO);
                return m;
            });
            row.put("contracts", ((Number) row.get("contracts")).intValue() + 1);
            row.put("amount", ((BigDecimal) row.get("amount"))
                    .add(contract.getAmount() == null ? BigDecimal.ZERO : contract.getAmount()));
            BigDecimal contractReceived = plans.stream().filter(p -> Objects.equals(p.getContractId(), contract.getId()))
                    .map(p -> p.getReceivedAmount() == null ? BigDecimal.ZERO : p.getReceivedAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            row.put("received", ((BigDecimal) row.get("received")).add(contractReceived));
        }
        List<Map<String, Object>> ranking = new ArrayList<>(rankingMap.values());
        ranking.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));

        LambdaQueryWrapper<ServiceTicket> ticketQw = new LambdaQueryWrapper<>();
        List<ServiceTicket> tickets = ticketMapper.selectList(ticketQw);
        Map<String, Long> ticketType = new LinkedHashMap<>();
        Map<String, Long> ticketStatus = new LinkedHashMap<>();
        for (String type : List.of("REPAIR", "INSTALL", "MAINTAIN", "INSPECT")) {
            ticketType.put(type, tickets.stream().filter(t -> type.equals(t.getType())).count());
        }
        for (String status : List.of("ACCEPTED", "DISPATCHED", "PROCESSING", "WAIT_REVIEW", "COMPLETED")) {
            ticketStatus.put(status, tickets.stream().filter(t -> status.equals(t.getStatus())).count());
        }
        double responseHours = tickets.stream().filter(t -> t.getAcceptedTime() != null && t.getCreatedAt() != null)
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getAcceptedTime()).toMinutes())
                .average().orElse(0) / 60.0;
        double satisfaction = tickets.stream().filter(t -> t.getSatisfactionScore() != null)
                .mapToInt(ServiceTicket::getSatisfactionScore).average().orElse(0);
        long partsOut = spareRecordMapper.selectList(new LambdaQueryWrapper<SpareRecord>().eq(SpareRecord::getType, "OUT"))
                .stream().mapToInt(r -> r.getQuantity() == null ? 0 : r.getQuantity()).sum();
        Map<String, Long> paymentStatus = new LinkedHashMap<>();
        for (String status : List.of("UNPAID", "PAID", "OVERDUE")) {
            paymentStatus.put(status, plans.stream().filter(p -> status.equals(p.getStatus())).count());
        }

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("contractAmount", amount);
        overview.put("received", received);
        overview.put("outstanding", outstanding);
        overview.put("receivableRate", amount.compareTo(BigDecimal.ZERO) == 0 ? 0
                : Math.round(received.doubleValue() * 10000 / amount.doubleValue()) / 100.0);
        overview.put("contracts", contracts.size());
        overview.put("demands", demands.size());
        overview.put("deals", deals);
        overview.put("conversionRate", demands.isEmpty() ? 0 : Math.round(deals * 10000.0 / demands.size()) / 100.0);
        overview.put("tickets", tickets.size());
        overview.put("avgSatisfaction", Math.round(satisfaction * 100) / 100.0);

        Map<String, Object> service = new LinkedHashMap<>();
        service.put("byType", ticketType);
        service.put("byStatus", ticketStatus);
        service.put("avgResponseHours", Math.round(responseHours * 100) / 100.0);
        service.put("avgSatisfaction", Math.round(satisfaction * 100) / 100.0);
        service.put("partsConsumed", partsOut);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", overview);
        result.put("funnel", funnel);
        result.put("monthly", monthly);
        result.put("ownerRanking", ranking);
        result.put("paymentStatus", paymentStatus);
        result.put("service", service);
        result.put("plans", plans);
        return ApiResult.ok(result);
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        Map<String, Object> data = report().getData();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> monthly = (List<Map<String, Object>>) data.get("monthly");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ranking = (List<Map<String, Object>>) data.get("ownerRanking");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> m : monthly) {
            rows.add(Arrays.asList(m.get("month"), m.get("contracts"), m.get("contractAmount"), m.get("received")));
        }
        rows.add(Arrays.asList("", "", "", ""));
        rows.add(Arrays.asList("业绩归属", "合同数", "成交额", "回款额"));
        for (Map<String, Object> r : ranking) {
            rows.add(Arrays.asList(r.get("name"), r.get("contracts"), r.get("amount"), r.get("received")));
        }
        excelService.export(response, "经营报表.xlsx", List.of("月份", "合同数", "成交额", "回款额"), rows);
        auditService.log("报表", "导出", null, null);
    }
}
