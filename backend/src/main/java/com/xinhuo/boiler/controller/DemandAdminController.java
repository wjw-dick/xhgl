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

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/demands")
@RequiredArgsConstructor
public class DemandAdminController {
    private final DemandMapper demandMapper;
    private final FollowRecordMapper followMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final ProductMapper productMapper;
    private final SolutionMapper solutionMapper;
    private final QuoteMapper quoteMapper;
    private final TenantUserMapper tenantUserMapper;
    private final IdGenerator idGenerator;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;
    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, List<String>> TRANSITIONS = Map.of(
            "NEW", List.of("CONSULTING", "LOST"),
            "CONSULTING", List.of("INTRODUCING", "LOST"),
            "INTRODUCING", List.of("TALKING", "LOST"),
            "TALKING", List.of("DEAL", "LOST"),
            "DEAL", List.of("TALKING"),
            "LOST", List.of("NEW")
    );

    private static final Map<String, String> PREVIOUS = Map.of(
            "CONSULTING", "NEW", "INTRODUCING", "CONSULTING", "TALKING", "INTRODUCING", "DEAL", "TALKING"
    );

    @GetMapping
    public ApiResult<PageResult<Demand>> list(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String source,
                                              @RequestParam(required = false) Long ownerId,
                                              @RequestParam(required = false) String unassigned) {
        var user = AuthContext.require();
        Page<Demand> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Demand> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Demand::getDemandNo, keyword)
                    .or().like(Demand::getCustomerId, keyword)
                    .or().like(Demand::getContactName, keyword)
                    .or().like(Demand::getRegion, keyword));
        }
        if (status != null && !status.isBlank()) qw.eq(Demand::getStatus, status);
        if (source != null && !source.isBlank()) qw.eq(Demand::getSource, source);
        if (ownerId != null) qw.eq(Demand::getOwnerId, ownerId);
        if ("true".equals(unassigned)) qw.isNull(Demand::getOwnerId);
        qw.orderByDesc(Demand::getId);
        return ApiResult.ok(PageResult.of(demandMapper.selectPage(p, qw)));
    }

    @GetMapping("/public-pool")
    public ApiResult<List<Demand>> publicPool() {
        return ApiResult.ok(demandMapper.selectList(new LambdaQueryWrapper<Demand>()
                .isNull(Demand::getOwnerId).ne(Demand::getStatus, "LOST").orderByDesc(Demand::getId).last("limit 100")));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Demand demand = find(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("demand", demand);
        result.put("customer", demand.getCustomerId() == null ? null : customerMapper.selectById(demand.getCustomerId()));
        result.put("contacts", demand.getCustomerId() == null ? List.of() : contactMapper.selectList(
                new LambdaQueryWrapper<Contact>().eq(Contact::getCustomerId, demand.getCustomerId())));
        result.put("followRecords", followMapper.selectList(new LambdaQueryWrapper<FollowRecord>()
                .eq(FollowRecord::getDemandId, id).orderByDesc(FollowRecord::getFollowTime)));
        result.put("solutions", solutionMapper.selectList(new LambdaQueryWrapper<Solution>()
                .eq(Solution::getDemandId, id).orderByDesc(Solution::getId)));
        result.put("quotes", quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getDemandId, id).orderByDesc(Quote::getId)));
        return ApiResult.ok(result);
    }

    @PostMapping
    public ApiResult<Demand> create(@RequestBody Demand demand) {
        var user = AuthContext.require();
        demand.setId(null);
        demand.setTenantId(user.getTenantId());
        demand.setDemandNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "DEMAND"));
        demand.setStatus("NEW");
        demand.setSource(demand.getSource() == null ? "MANUAL" : demand.getSource());
        if (demand.getFollowNextTime() == null) demand.setFollowNextTime(LocalDateTime.now().plusDays(2));
        demand.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        demandMapper.insert(demand);
        auditService.log("需求线索", "新增", demand.getDemandNo(), null);
        return ApiResult.ok(demand);
    }

    @PutMapping("/{id}")
    public ApiResult<Demand> update(@PathVariable Long id, @RequestBody Demand demand) {
        Demand existing = find(id);
        demand.setId(id);
        demand.setTenantId(existing.getTenantId());
        demand.setDemandNo(existing.getDemandNo());
        demand.setOwnerId(existing.getOwnerId());
        demand.setOwnerName(existing.getOwnerName());
        demand.setStatus(existing.getStatus());
        demandMapper.updateById(demand);
        auditService.log("需求线索", "修改", existing.getDemandNo(), null);
        return ApiResult.ok(demandMapper.selectById(id));
    }

    @PostMapping("/{id}/claim")
    public ApiResult<Demand> claim(@PathVariable Long id) {
        var user = AuthContext.require();
        Demand demand = find(id);
        if (demand.getOwnerId() != null && !demand.getOwnerId().equals(user.getId())) {
            throw new BizException(403, "需求已被其他人员认领");
        }
        demand.setOwnerId(user.getId());
        demand.setOwnerName(user.getName());
        demandMapper.updateById(demand);
        addFollow(id, "SYSTEM", "认领需求：" + user.getName(), null, user);
        auditService.log("需求线索", "认领", demand.getDemandNo(), null);
        return ApiResult.ok(demand);
    }

    @PostMapping("/{id}/assign")
    public ApiResult<Demand> assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var user = AuthContext.require();
        if (!user.isAdmin()) throw new BizException(403, "仅管理员可分配需求");
        Demand demand = find(id);
        Long ownerId = Long.valueOf(String.valueOf(body.get("ownerId")));
        TenantUser owner = tenantUserMapper.selectById(ownerId);
        if (owner == null || !owner.getTenantId().equals(user.getTenantId())) throw new BizException(404, "负责人不存在");
        demand.setOwnerId(ownerId);
        demand.setOwnerName(owner.getName());
        String remark = String.valueOf(body.getOrDefault("remark", ""));
        demand.setAssignRemark(remark);
        demandMapper.updateById(demand);
        addFollow(id, "SYSTEM", "管理员分配给 " + owner.getName() + (remark.isBlank() ? "" : "：" + remark), null, user);
        notificationService.notifyTenant(user.getTenantId(), ownerId, "新需求分配",
                "需求 " + demand.getDemandNo() + " 已分配给您，请及时跟进", "DEMAND_ASSIGN", "/demands");
        auditService.log("需求线索", "分配", demand.getDemandNo(), "分配给 " + owner.getName());
        return ApiResult.ok(demand);
    }

    @PostMapping("/{id}/transfer")
    public ApiResult<Demand> transfer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var user = AuthContext.require();
        Demand demand = find(id);
        Long ownerId = Long.valueOf(String.valueOf(body.get("ownerId")));
        TenantUser owner = tenantUserMapper.selectById(ownerId);
        if (owner == null || !owner.getTenantId().equals(user.getTenantId())) throw new BizException(404, "负责人不存在");
        demand.setOwnerId(ownerId);
        demand.setOwnerName(owner.getName());
        demandMapper.updateById(demand);
        addFollow(id, "SYSTEM", "需求转派给 " + owner.getName(), null, user);
        auditService.log("需求线索", "转派", demand.getDemandNo(), "转派给 " + owner.getName());
        return ApiResult.ok(demand);
    }

    @GetMapping("/{id}/follows")
    public ApiResult<List<FollowRecord>> follows(@PathVariable Long id) {
        find(id);
        return ApiResult.ok(followMapper.selectList(new LambdaQueryWrapper<FollowRecord>()
                .eq(FollowRecord::getDemandId, id).orderByDesc(FollowRecord::getFollowTime)));
    }

    @PostMapping("/{id}/follow")
    public ApiResult<FollowRecord> follow(@PathVariable Long id, @RequestBody FollowRecord record) {
        var user = AuthContext.require();
        Demand demand = find(id);
        if (record.getContent() == null || record.getContent().isBlank()) throw new BizException("跟进内容不能为空");
        record.setId(null);
        record.setTenantId(user.getTenantId());
        record.setDemandId(id);
        record.setUserId(user.getId());
        record.setUserName(user.getName());
        record.setFollowTime(LocalDateTime.now());
        followMapper.insert(record);
        demand.setLastFollowTime(LocalDateTime.now());
        demand.setFollowNextTime(record.getNextTime());
        demandMapper.updateById(demand);
        auditService.log("需求线索", "跟进", demand.getDemandNo(), record.getContent());
        return ApiResult.ok(record);
    }

    @PostMapping("/{id}/status")
    public ApiResult<Demand> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var user = AuthContext.require();
        Demand demand = find(id);
        String target = body.get("status");
        List<String> allowed = new ArrayList<>(TRANSITIONS.getOrDefault(demand.getStatus(), List.of()));
        String previous = PREVIOUS.get(demand.getStatus());
        if (previous != null) allowed.add(previous);
        if (!allowed.contains(target)) throw new BizException("不允许从 " + demand.getStatus() + " 流转到 " + target);
        String old = demand.getStatus();
        demand.setStatus(target);
        demandMapper.updateById(demand);
        String remark = body.getOrDefault("remark", "");
        addFollow(id, "SYSTEM", "状态流转：" + old + " → " + target + (remark.isBlank() ? "" : "；" + remark), null, user);
        auditService.log("需求线索", "状态流转", demand.getDemandNo(), old + "→" + target);
        if ("DEAL".equals(target)) {
            notificationService.notifyTenant(user.getTenantId(), demand.getOwnerId(), "需求成交", demand.getDemandNo() + " 已成交", "DEMAND_DEAL", "/demands");
        }
        return ApiResult.ok(demand);
    }

    @GetMapping("/funnel")
    public ApiResult<Map<String, Object>> funnel() {
        LambdaQueryWrapper<Demand> base = new LambdaQueryWrapper<>();
        long total = demandMapper.selectCount(new LambdaQueryWrapper<>());
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : List.of("NEW", "CONSULTING", "INTRODUCING", "TALKING", "DEAL", "LOST")) {
            counts.put(status, demandMapper.selectCount(base.clone().eq(Demand::getStatus, status)));
        }
        long deal = counts.getOrDefault("DEAL", 0L);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("counts", counts);
        result.put("conversionRate", total == 0 ? 0 : Math.round(deal * 10000.0 / total) / 100.0);
        return ApiResult.ok(result);
    }

    @GetMapping("/duplicate")
    public ApiResult<Map<String, Object>> duplicate(@RequestParam(required = false) String name,
                                                     @RequestParam(required = false) String phone) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Customer> customers = new ArrayList<>();
        if (name != null && !name.isBlank()) {
            customers = customerMapper.selectList(new LambdaQueryWrapper<Customer>().eq(Customer::getName, name));
        }
        List<Demand> demands = new ArrayList<>();
        if (!customers.isEmpty()) {
            demands = demandMapper.selectList(new LambdaQueryWrapper<Demand>()
                    .in(Demand::getCustomerId, customers.stream().map(Customer::getId).toList())
                    .notIn(Demand::getStatus, "LOST", "DEAL").orderByDesc(Demand::getId).last("limit 5"));
        }
        result.put("duplicate", !demands.isEmpty());
        result.put("demands", demands);
        return ApiResult.ok(result);
    }

    @GetMapping("/recommend")
    public ApiResult<List<Map<String, Object>>> recommend(@RequestParam Long demandId) {
        Demand demand = find(demandId);
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getOnlineStatus, "ONLINE").orderByAsc(Product::getId));
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Product product : products) {
            int score = 20;
            if (Objects.equals(demand.getBoilerType(), product.getBoilerType())) score += 30;
            if (Objects.equals(demand.getFuelType(), product.getFuelType())) score += 25;
            if (demand.getCapacity() != null && product.getCapacity() != null
                    && product.getCapacity().replace(" ", "").contains(demand.getCapacity().replace(" ", ""))) score += 20;
            if (Objects.equals(demand.getPressureLevel(), product.getPressureLevel())) score += 10;
            if ("OK".equals(product.getComplianceStatus())) score += 10;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("product", product);
            item.put("score", score);
            scored.add(item);
        }
        scored.sort((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score")));
        List<Map<String, Object>> result = scored.stream().limit(5).toList();
        auditService.log("产品方案", "选型推荐", demand.getDemandNo(), "推荐 " + result.size() + " 个型号");
        return ApiResult.ok(result);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResult<Void> delete(@PathVariable Long id) {
        Demand demand = find(id);
        if (solutionMapper.selectCount(new LambdaQueryWrapper<Solution>().eq(Solution::getDemandId, id)) > 0
                || quoteMapper.selectCount(new LambdaQueryWrapper<Quote>().eq(Quote::getDemandId, id)) > 0) {
            throw new BizException(409, "该需求已关联方案或报价，请先删除方案/报价后再删除需求");
        }
        jdbcTemplate.update("UPDATE consultation SET demand_id = NULL WHERE demand_id = ?", id);
        jdbcTemplate.update("DELETE FROM follow_record WHERE demand_id = ?", id);
        jdbcTemplate.update("DELETE FROM demand WHERE id = ?", id);
        auditService.log("需求线索", "删除", demand.getDemandNo(), demand.getCustomerId() == null ? null : String.valueOf(demand.getCustomerId()));
        return ApiResult.ok();
    }

    private Demand find(Long id) {
        Demand demand = demandMapper.selectById(id);
        if (demand == null) throw new BizException(404, "需求不存在");
        return demand;
    }

    private void addFollow(Long demandId, String method, String content, LocalDateTime next, AuthUser user) {
        FollowRecord record = new FollowRecord();
        record.setTenantId(user.getTenantId());
        record.setDemandId(demandId);
        record.setUserId(user.getId());
        record.setUserName(user.getName());
        record.setFollowTime(LocalDateTime.now());
        record.setMethod(method);
        record.setContent(content);
        record.setNextTime(next);
        followMapper.insert(record);
    }
}
