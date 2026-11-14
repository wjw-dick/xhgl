package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
public class TicketAdminController {
    private final ServiceTicketMapper ticketMapper;
    private final TicketRecordMapper recordMapper;
    private final CustomerMapper customerMapper;
    private final EquipmentMapper equipmentMapper;
    private final TenantUserMapper userMapper;
    private final SpareRecordMapper spareRecordMapper;
    private final SatisfactionMapper satisfactionMapper;
    private final IdGenerator idGenerator;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, List<String>> TRANSITIONS = Map.of(
            "ACCEPTED", List.of("DISPATCHED", "PROCESSING"),
            "DISPATCHED", List.of("PROCESSING"),
            "PROCESSING", List.of("WAIT_REVIEW"),
            "WAIT_REVIEW", List.of("COMPLETED"),
            "COMPLETED", List.of()
    );

    @GetMapping
    public ApiResult<PageResult<ServiceTicket>> list(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String type,
                                                     @RequestParam(required = false) String priority,
                                                     @RequestParam(required = false) Long customerId) {
        var user = AuthContext.require();
        Page<ServiceTicket> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<ServiceTicket> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) qw.and(w -> w.like(ServiceTicket::getTicketNo, keyword)
                .or().like(ServiceTicket::getCustomerName, keyword)
                .or().like(ServiceTicket::getEquipmentNo, keyword)
                .or().like(ServiceTicket::getDescription, keyword));
        if (status != null && !status.isBlank()) qw.eq(ServiceTicket::getStatus, status);
        if (type != null && !type.isBlank()) qw.eq(ServiceTicket::getType, type);
        if (priority != null && !priority.isBlank()) qw.eq(ServiceTicket::getPriority, priority);
        if (customerId != null) qw.eq(ServiceTicket::getCustomerId, customerId);
        if ("TECHNICIAN".equals(user.getRoleCode())) qw.eq(ServiceTicket::getTechnicianId, user.getId());
        qw.orderByDesc(ServiceTicket::getId);
        return ApiResult.ok(PageResult.of(ticketMapper.selectPage(p, qw)));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        ServiceTicket ticket = find(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticket", ticket);
        result.put("customer", ticket.getCustomerId() == null ? null : customerMapper.selectById(ticket.getCustomerId()));
        result.put("equipment", ticket.getEquipmentId() == null ? null : equipmentMapper.selectById(ticket.getEquipmentId()));
        result.put("records", recordMapper.selectList(new LambdaQueryWrapper<TicketRecord>()
                .eq(TicketRecord::getTicketId, id).orderByAsc(TicketRecord::getCreatedAt)));
        result.put("technicians", userMapper.selectList(new LambdaQueryWrapper<TenantUser>()
                .in(TenantUser::getRoleCode, "TECHNICIAN", "SERVICE_MANAGER").eq(TenantUser::getStatus, "NORMAL")));
        return ApiResult.ok(result);
    }

    @PostMapping
    public ApiResult<ServiceTicket> create(@RequestBody ServiceTicket ticket) {
        var user = AuthContext.require();
        if (ticket.getCustomerId() == null) throw new BizException("请选择客户");
        Customer customer = customerMapper.selectById(ticket.getCustomerId());
        if (customer == null) throw new BizException(404, "客户不存在");
        for (String active : List.of("ACCEPTED", "DISPATCHED", "PROCESSING", "WAIT_REVIEW")) {
            ServiceTicket existing = ticketMapper.selectOne(new LambdaQueryWrapper<ServiceTicket>()
                    .eq(ServiceTicket::getCustomerId, ticket.getCustomerId())
                    .eq(ticket.getEquipmentId() != null, ServiceTicket::getEquipmentId, ticket.getEquipmentId())
                    .eq(ServiceTicket::getType, ticket.getType())
                    .eq(ServiceTicket::getStatus, active)
                    .ge(ServiceTicket::getCreatedAt, LocalDateTime.now().minusHours(24)).last("limit 1"));
            if (existing != null) {
                throw new BizException(409, "存在24小时内同类未完结工单 " + existing.getTicketNo() + "，已合并提示");
            }
        }
        ticket.setId(null);
        ticket.setTenantId(user.getTenantId());
        ticket.setTicketNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "TICKET"));
        ticket.setCustomerName(customer.getName());
        ticket.setStatus("ACCEPTED");
        ticket.setAcceptedTime(LocalDateTime.now());
        if (ticket.getOwnerId() == null) ticket.setOwnerId(customer.getOwnerId());
        if (ticket.getSource() == null) ticket.setSource("PHONE");
        if (ticket.getPriority() == null) ticket.setPriority("NORMAL");
        if (ticket.getEquipmentId() != null) {
            Equipment equipment = equipmentMapper.selectById(ticket.getEquipmentId());
            if (equipment != null) ticket.setEquipmentNo(equipment.getEquipmentNo());
        }
        ticketMapper.insert(ticket);
        addRecord(ticket.getId(), user.getName(), "受理", "报修已受理", null, null);
        auditService.log("售后服务", "新建工单", ticket.getTicketNo(), ticket.getDescription());
        return ApiResult.ok(ticket);
    }

    @PostMapping("/{id}/dispatch")
    public ApiResult<ServiceTicket> dispatch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var user = AuthContext.require();
        ServiceTicket ticket = find(id);
        if (!"ACCEPTED".equals(ticket.getStatus())) throw new BizException("仅已受理工单可派单");
        Long techId = Long.valueOf(String.valueOf(body.get("technicianId")));
        TenantUser tech = userMapper.selectById(techId);
        if (tech == null) throw new BizException(404, "技师不存在");
        ticket.setTechnicianId(techId);
        ticket.setTechnicianName(tech.getName());
        ticket.setStatus("DISPATCHED");
        ticketMapper.updateById(ticket);
        addRecord(id, user.getName(), "派单", "派单给 " + tech.getName(), null, null);
        notificationService.notifyTenant(user.getTenantId(), techId, "新工单派单",
                "工单 " + ticket.getTicketNo() + " 已派给您", "TICKET_NOTICE", "/tickets");
        if (ticket.getCustomerId() != null) {
            notificationService.notifyCustomer(user.getTenantId(), ticket.getCustomerId(), "TICKET_NOTICE",
                    "售后工单已派单", "工单 " + ticket.getTicketNo() + " 技师 " + tech.getName() + " 已接单",
                    Map.of("ticketNo", ticket.getTicketNo(), "status", "已派单", "technician", tech.getName()), "/account");
        }
        auditService.log("售后服务", "派单", ticket.getTicketNo(), tech.getName());
        return ApiResult.ok(ticket);
    }

    @PostMapping("/{id}/accept")
    public ApiResult<ServiceTicket> accept(@PathVariable Long id) {
        var user = AuthContext.require();
        ServiceTicket ticket = find(id);
        if (!"DISPATCHED".equals(ticket.getStatus()) && !"ACCEPTED".equals(ticket.getStatus())) {
            throw new BizException("当前状态不可接单");
        }
        if ("TECHNICIAN".equals(user.getRoleCode()) && ticket.getTechnicianId() != null
                && !ticket.getTechnicianId().equals(user.getId())) {
            throw new BizException(403, "非本人派单工单");
        }
        if (ticket.getTechnicianId() == null) {
            ticket.setTechnicianId(user.getId());
            ticket.setTechnicianName(user.getName());
        }
        ticket.setStatus("PROCESSING");
        ticket.setAcceptedTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        addRecord(id, user.getName(), "接单", "技师已接单", null, null);
        auditService.log("售后服务", "接单", ticket.getTicketNo(), user.getName());
        return ApiResult.ok(ticket);
    }

    @PostMapping("/{id}/records")
    public ApiResult<TicketRecord> addRecordApi(@PathVariable Long id, @RequestBody TicketRecord record) {
        var user = AuthContext.require();
        ServiceTicket ticket = find(id);
        if (ticket.getTechnicianId() != null && "TECHNICIAN".equals(user.getRoleCode())
                && !ticket.getTechnicianId().equals(user.getId())) {
            throw new BizException(403, "仅可记录本人工单");
        }
        addRecord(id, user.getName(), record.getAction(), record.getContent(), record.getPhotos(), record.getPartsJson());
        TicketRecord saved = recordMapper.selectOne(new LambdaQueryWrapper<TicketRecord>()
                .eq(TicketRecord::getTicketId, id).orderByDesc(TicketRecord::getId).last("limit 1"));
        auditService.log("售后服务", "现场记录", ticket.getTicketNo(), record.getContent());
        return ApiResult.ok(saved);
    }

    @PostMapping("/{id}/complete")
    public ApiResult<ServiceTicket> complete(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var user = AuthContext.require();
        ServiceTicket ticket = find(id);
        if (!"PROCESSING".equals(ticket.getStatus())) throw new BizException("仅处理中工单可完工");
        ticket.setStatus("WAIT_REVIEW");
        ticket.setDoneTime(LocalDateTime.now());
        ticket.setSolution(body.getOrDefault("solution", ""));
        ticketMapper.updateById(ticket);
        addRecord(id, user.getName(), "完工", body.getOrDefault("solution", "工单完工"), body.get("photos"), body.get("partsJson"));
        if (ticket.getCustomerId() != null) {
            notificationService.notifyCustomer(user.getTenantId(), ticket.getCustomerId(), "VISIT_INVITE",
                    "服务已完成，邀请评价", "工单 " + ticket.getTicketNo() + " 已完工，请对本次服务进行评价",
                    Map.of("contractNo", "", "reviewLink", "/account"), "/account");
        }
        auditService.log("售后服务", "完工", ticket.getTicketNo(), null);
        return ApiResult.ok(ticket);
    }

    @PostMapping("/{id}/review")
    public ApiResult<ServiceTicket> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var user = AuthContext.require();
        ServiceTicket ticket = find(id);
        if (!"WAIT_REVIEW".equals(ticket.getStatus()) && !"COMPLETED".equals(ticket.getStatus())) {
            throw new BizException("当前状态不可评价");
        }
        int score = Integer.parseInt(String.valueOf(body.getOrDefault("score", 5)));
        String comment = String.valueOf(body.getOrDefault("comment", ""));
        ticket.setStatus("COMPLETED");
        ticket.setSatisfactionScore(score);
        ticket.setReview(comment);
        ticketMapper.updateById(ticket);
        Satisfaction satisfaction = new Satisfaction();
        satisfaction.setTenantId(user.getTenantId());
        satisfaction.setTicketId(id);
        satisfaction.setCustomerId(ticket.getCustomerId());
        satisfaction.setScore(score);
        satisfaction.setComment(comment);
        satisfaction.setSource("ADMIN");
        satisfactionMapper.insert(satisfaction);
        addRecord(id, user.getName(), "评价", score + "星：" + comment, null, null);
        auditService.log("售后服务", "满意度评价", ticket.getTicketNo(), score + "星");
        return ApiResult.ok(ticket);
    }

    @GetMapping("/analysis")
    public ApiResult<Map<String, Object>> analysis() {
        List<ServiceTicket> all = ticketMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String status : List.of("ACCEPTED", "DISPATCHED", "PROCESSING", "WAIT_REVIEW", "COMPLETED")) {
            byStatus.put(status, all.stream().filter(t -> status.equals(t.getStatus())).count());
        }
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : List.of("REPAIR", "INSTALL", "MAINTAIN", "INSPECT")) {
            byType.put(type, all.stream().filter(t -> type.equals(t.getType())).count());
        }
        double avgSatisfaction = all.stream().filter(t -> t.getSatisfactionScore() != null)
                .mapToInt(ServiceTicket::getSatisfactionScore).average().orElse(0);
        double avgResponseHours = all.stream().filter(t -> t.getAcceptedTime() != null && t.getCreatedAt() != null)
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getAcceptedTime()).toMinutes())
                .average().orElse(0) / 60.0;
        long completed = all.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long repeat = 0;
        for (ServiceTicket ticket : all) {
            if (!"COMPLETED".equals(ticket.getStatus()) || ticket.getDoneTime() == null) continue;
            long later = all.stream().filter(t -> t.getEquipmentId() != null
                    && t.getEquipmentId().equals(ticket.getEquipmentId()) && t.getId() > ticket.getId()
                    && t.getCreatedAt() != null && t.getCreatedAt().isBefore(ticket.getDoneTime().plusDays(7))).count();
            if (later > 0) repeat++;
        }
        double firstResolutionRate = completed == 0 ? 100 : Math.round((completed - repeat) * 10000.0 / completed) / 100.0;
        long parts = spareRecordMapper.selectList(new LambdaQueryWrapper<SpareRecord>()
                .eq(SpareRecord::getType, "OUT")).stream().mapToInt(r -> r.getQuantity() == null ? 0 : r.getQuantity()).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("byStatus", byStatus);
        result.put("byType", byType);
        result.put("avgSatisfaction", Math.round(avgSatisfaction * 100) / 100.0);
        result.put("avgResponseHours", Math.round(avgResponseHours * 100) / 100.0);
        result.put("firstResolutionRate", firstResolutionRate);
        result.put("partsConsumed", parts);
        return ApiResult.ok(result);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResult<Void> delete(@PathVariable Long id) {
        ServiceTicket ticket = find(id);
        jdbcTemplate.update("DELETE FROM spare_record WHERE ticket_id = ?", id);
        jdbcTemplate.update("DELETE FROM satisfaction WHERE ticket_id = ?", id);
        jdbcTemplate.update("DELETE FROM ticket_record WHERE ticket_id = ?", id);
        jdbcTemplate.update("DELETE FROM service_ticket WHERE id = ?", id);
        auditService.log("售后服务", "删除", ticket.getTicketNo(), ticket.getDescription());
        return ApiResult.ok();
    }

    private ServiceTicket find(Long id) {
        ServiceTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) throw new BizException(404, "工单不存在");
        var user = AuthContext.require();
        if ("TECHNICIAN".equals(user.getRoleCode()) && ticket.getTechnicianId() != null
                && !ticket.getTechnicianId().equals(user.getId())) {
            throw new BizException(403, "无权访问非本人工单");
        }
        return ticket;
    }

    private void addRecord(Long ticketId, String operator, String action, String content, String photos, String parts) {
        var user = AuthContext.require();
        TicketRecord record = new TicketRecord();
        record.setTenantId(user.getTenantId());
        record.setTicketId(ticketId);
        record.setOperator(operator);
        record.setAction(action);
        record.setContent(content);
        record.setPhotos(photos);
        record.setPartsJson(parts);
        recordMapper.insert(record);
    }
}
