package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.Consultation;
import com.xinhuo.boiler.entity.Customer;
import com.xinhuo.boiler.entity.Demand;
import com.xinhuo.boiler.entity.Faq;
import com.xinhuo.boiler.mapper.ConsultationMapper;
import com.xinhuo.boiler.mapper.CustomerMapper;
import com.xinhuo.boiler.mapper.DemandMapper;
import com.xinhuo.boiler.mapper.FaqMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.IdGenerator;
import com.xinhuo.boiler.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/consultations")
@RequiredArgsConstructor
public class ConsultationAdminController {
    private final ConsultationMapper consultationMapper;
    private final DemandMapper demandMapper;
    private final CustomerMapper customerMapper;
    private final FaqMapper faqMapper;
    private final IdGenerator idGenerator;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ApiResult<PageResult<Consultation>> list(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String channel) {
        Page<Consultation> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Consultation> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Consultation::getQuestion, keyword)
                    .or().like(Consultation::getAnswer, keyword)
                    .or().like(Consultation::getCustomerName, keyword));
        }
        if (status != null && !status.isBlank()) qw.eq(Consultation::getStatus, status);
        if (channel != null && !channel.isBlank()) qw.eq(Consultation::getChannel, channel);
        qw.orderByDesc(Consultation::getId);
        return ApiResult.ok(PageResult.of(consultationMapper.selectPage(p, qw)));
    }

    @GetMapping("/{id}")
    public ApiResult<Consultation> detail(@PathVariable Long id) {
        Consultation c = consultationMapper.selectById(id);
        if (c == null) throw new BizException(404, "咨询记录不存在");
        return ApiResult.ok(c);
    }

    @PostMapping
    public ApiResult<Consultation> create(@RequestBody Consultation consultation) {
        var user = AuthContext.require();
        consultation.setId(null);
        consultation.setTenantId(user.getTenantId());
        consultation.setConsultantId(user.getId());
        consultation.setConsultantName(user.getName());
        consultation.setStatus(consultation.getStatus() == null ? "WAITING" : consultation.getStatus());
        if (consultation.getDemandId() != null) {
            Demand demand = demandMapper.selectById(consultation.getDemandId());
            if (demand == null) throw new BizException(404, "需求不存在");
            if (consultation.getCustomerId() == null) consultation.setCustomerId(demand.getCustomerId());
        }
        if (consultation.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(consultation.getCustomerId());
            if (customer != null) consultation.setCustomerName(customer.getName());
        }
        consultationMapper.insert(consultation);
        auditService.log("售前咨询", "新增", String.valueOf(consultation.getId()), consultation.getQuestion());
        return ApiResult.ok(consultation);
    }

    @PutMapping("/{id}")
    public ApiResult<Consultation> update(@PathVariable Long id, @RequestBody Consultation consultation) {
        Consultation existing = consultationMapper.selectById(id);
        if (existing == null) throw new BizException(404, "咨询记录不存在");
        consultation.setId(id);
        consultation.setTenantId(existing.getTenantId());
        consultationMapper.updateById(consultation);
        return ApiResult.ok(consultationMapper.selectById(id));
    }

    @PostMapping("/{id}/reply")
    public ApiResult<Consultation> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var user = AuthContext.require();
        Consultation consultation = consultationMapper.selectById(id);
        if (consultation == null) throw new BizException(404, "咨询记录不存在");
        String answer = body.get("answer");
        if (answer == null || answer.isBlank()) throw new BizException("答复内容不能为空");
        consultation.setAnswer(answer);
        consultation.setStatus("DONE");
        consultation.setConsultantId(user.getId());
        consultation.setConsultantName(user.getName());
        consultationMapper.updateById(consultation);
        auditService.log("售前咨询", "答复", String.valueOf(id), answer);
        if (consultation.getCustomerId() != null) {
            notificationService.notifyCustomer(user.getTenantId(), consultation.getCustomerId(), "CONSULT_REPLY",
                    "顾问已回复您的咨询", answer, Map.of("demandNo", String.valueOf(consultation.getDemandId()),
                            "answer", answer, "link", "/account"), "/account");
        }
        return ApiResult.ok(consultation);
    }

    @PostMapping("/{id}/to-demand")
    public ApiResult<Demand> toDemand(@PathVariable Long id, @RequestBody Demand demand) {
        var user = AuthContext.require();
        Consultation consultation = consultationMapper.selectById(id);
        if (consultation == null) throw new BizException(404, "咨询记录不存在");
        demand.setId(null);
        demand.setTenantId(user.getTenantId());
        demand.setDemandNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "DEMAND"));
        demand.setCustomerId(consultation.getCustomerId());
        demand.setContactName(consultation.getCustomerName());
        demand.setSource(consultation.getChannel());
        demand.setOwnerId(user.getId());
        demand.setOwnerName(user.getName());
        demand.setStatus("CONSULTING");
        demand.setExpectTime(demand.getExpectTime() == null ? LocalDate.now().plusDays(30) : demand.getExpectTime());
        demand.setFollowNextTime(LocalDateTime.now().plusDays(2));
        demand.setShareToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        demandMapper.insert(demand);
        consultation.setDemandId(demand.getId());
        consultation.setStatus("CONVERTED");
        consultationMapper.updateById(consultation);
        auditService.log("售前咨询", "转需求", consultation.getCustomerName(), demand.getDemandNo());
        return ApiResult.ok(demand);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Consultation consultation = consultationMapper.selectById(id);
        if (consultation == null) throw new BizException(404, "咨询记录不存在");
        jdbcTemplate.update("DELETE FROM consultation WHERE id = ?", id);
        auditService.log("售前咨询", "删除", consultation.getCustomerName(), consultation.getQuestion());
        return ApiResult.ok();
    }

    @GetMapping("/faq-suggest")
    public ApiResult<List<Faq>> faqSuggest(@RequestParam String question) {
        List<Faq> all = faqMapper.selectList(new LambdaQueryWrapper<Faq>()
                .eq(Faq::getStatus, "NORMAL").orderByAsc(Faq::getId));
        String q = question == null ? "" : question.toLowerCase();
        List<Faq> matched = all.stream().filter(f -> (f.getQuestion() + " " + f.getKeywords()).toLowerCase()
                .contains(q.substring(0, Math.min(q.length(), 8)))).limit(5).toList();
        return ApiResult.ok(matched.isEmpty() ? all.stream().limit(3).toList() : matched);
    }
}
