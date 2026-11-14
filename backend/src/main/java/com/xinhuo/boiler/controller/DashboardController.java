package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final DemandMapper demandMapper;
    private final ContractMapper contractMapper;
    private final PaymentPlanMapper paymentMapper;
    private final ServiceTicketMapper ticketMapper;
    private final MaintenancePlanMapper maintenanceMapper;
    private final CertificateMapper certificateMapper;

    @GetMapping
    public ApiResult<Map<String, Object>> dashboard() {
        LambdaQueryWrapper<Customer> customerQuery = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Demand> demandQuery = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Contract> contractQuery = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<ServiceTicket> ticketQuery = new LambdaQueryWrapper<>();

        long customers = customerMapper.selectCount(customerQuery);
        long products = productMapper.selectCount(new LambdaQueryWrapper<>());
        long demands = demandMapper.selectCount(demandQuery);
        long activeDemands = demandMapper.selectCount(demandQuery.clone()
                .notIn(Demand::getStatus, "DEAL", "LOST"));
        long contracts = contractMapper.selectCount(contractQuery);
        List<Contract> contractList = contractMapper.selectList(contractQuery.clone().orderByDesc(Contract::getId));
        BigDecimal contractAmount = contractList.stream().map(c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Long> contractIds = contractList.stream().map(Contract::getId).toList();
        List<PaymentPlan> plans = contractIds.isEmpty() ? List.of() : paymentMapper.selectList(
                new LambdaQueryWrapper<PaymentPlan>().in(PaymentPlan::getContractId, contractIds));
        BigDecimal received = plans.stream().map(p -> p.getReceivedAmount() == null ? BigDecimal.ZERO : p.getReceivedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = plans.stream().filter(p -> !"PAID".equals(p.getStatus()))
                .map(p -> {
                    BigDecimal amount = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount();
                    BigDecimal got = p.getReceivedAmount() == null ? BigDecimal.ZERO : p.getReceivedAmount();
                    return amount.subtract(got);
                }).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> funnel = new LinkedHashMap<>();
        for (String status : List.of("NEW", "CONSULTING", "INTRODUCING", "TALKING", "DEAL", "LOST")) {
            funnel.put(status, demandMapper.selectCount(demandQuery.clone().eq(Demand::getStatus, status)));
        }
        Map<String, Long> ticketStatus = new LinkedHashMap<>();
        for (String status : List.of("ACCEPTED", "DISPATCHED", "PROCESSING", "WAIT_REVIEW", "COMPLETED")) {
            ticketStatus.put(status, ticketMapper.selectCount(ticketQuery.clone().eq(ServiceTicket::getStatus, status)));
        }
        long pendingTickets = ticketStatus.values().stream().mapToLong(Long::longValue).sum()
                - ticketStatus.getOrDefault("COMPLETED", 0L);
        double avgSatisfaction = ticketMapper.selectList(ticketQuery.clone().isNotNull(ServiceTicket::getSatisfactionScore))
                .stream().mapToInt(ServiceTicket::getSatisfactionScore).average().orElse(0);

        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            BigDecimal monthAmount = contractList.stream()
                    .filter(c -> c.getCreatedAt() != null && YearMonth.from(c.getCreatedAt()).equals(month))
                    .map(c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal monthReceived = plans.stream()
                    .filter(p -> p.getReceivedDate() != null && YearMonth.from(p.getReceivedDate()).equals(month))
                    .map(p -> p.getReceivedAmount() == null ? BigDecimal.ZERO : p.getReceivedAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            trend.add(Map.of("month", month.format(monthFormatter), "contractAmount", monthAmount,
                    "received", monthReceived, "contracts", contractList.stream()
                            .filter(c -> c.getCreatedAt() != null && YearMonth.from(c.getCreatedAt()).equals(month)).count()));
        }

        long dueMaintenance = maintenanceMapper.selectCount(new LambdaQueryWrapper<MaintenancePlan>()
                .eq(MaintenancePlan::getStatus, "ACTIVE")
                .le(MaintenancePlan::getNextDate, LocalDate.now().plusDays(30)));
        long dueCertificates = certificateMapper.selectCount(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getStatus, "VALID")
                .le(Certificate::getValidTo, LocalDate.now().plusDays(90)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customers", customers);
        result.put("products", products);
        result.put("demands", demands);
        result.put("activeDemands", activeDemands);
        result.put("contracts", contracts);
        result.put("contractAmount", contractAmount);
        result.put("received", received);
        result.put("outstanding", outstanding);
        result.put("pendingTickets", pendingTickets);
        result.put("avgSatisfaction", Math.round(avgSatisfaction * 100) / 100.0);
        result.put("funnel", funnel);
        result.put("ticketStatus", ticketStatus);
        result.put("trend", trend);
        result.put("dueMaintenance", dueMaintenance);
        result.put("dueCertificates", dueCertificates);
        result.put("recentDemands", demandMapper.selectList(demandQuery.clone().orderByDesc(Demand::getId).last("limit 8")));
        result.put("recentTickets", ticketMapper.selectList(ticketQuery.clone().orderByDesc(ServiceTicket::getId).last("limit 8")));
        return ApiResult.ok(result);
    }
}
