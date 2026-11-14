package com.xinhuo.boiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinhuo.boiler.common.*;
import com.xinhuo.boiler.entity.*;
import com.xinhuo.boiler.mapper.*;
import com.xinhuo.boiler.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/solutions")
@RequiredArgsConstructor
public class SolutionAdminController {
    private final SolutionMapper solutionMapper;
    private final DemandMapper demandMapper;
    private final ProductMapper productMapper;
    private final CustomerMapper customerMapper;
    private final IdGenerator idGenerator;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final QuoteMapper quoteMapper;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ApiResult<PageResult<Solution>> list(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) Long demandId) {
        var user = AuthContext.require();
        Page<Solution> p = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Solution> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Solution::getName, keyword).or().like(Solution::getSolutionNo, keyword)
                    .or().like(Solution::getProductName, keyword));
        }
        if (status != null && !status.isBlank()) qw.eq(Solution::getStatus, status);
        if (demandId != null) qw.eq(Solution::getDemandId, demandId);
        qw.orderByDesc(Solution::getId);
        return ApiResult.ok(PageResult.of(solutionMapper.selectPage(p, qw)));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Solution solution = find(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("solution", solution);
        result.put("demand", solution.getDemandId() == null ? null : demandMapper.selectById(solution.getDemandId()));
        result.put("product", solution.getProductId() == null ? null : productMapper.selectById(solution.getProductId()));
        result.put("versions", solutionMapper.selectList(new LambdaQueryWrapper<Solution>()
                .eq(Solution::getDemandId, solution.getDemandId()).orderByDesc(Solution::getVersion)));
        return ApiResult.ok(result);
    }

    @PostMapping
    public ApiResult<Solution> create(@RequestBody Solution solution) {
        var user = AuthContext.require();
        Demand demand = requireDemand(solution.getDemandId());
        solution.setId(null);
        solution.setTenantId(user.getTenantId());
        solution.setSolutionNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "SOLUTION"));
        solution.setVersion(1);
        solution.setStatus("DRAFT");
        solution.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        solution.setViewCount(0);
        solution.setCreatedBy(user.getId());
        fillProduct(solution);
        solutionMapper.insert(solution);
        auditService.log("产品方案", "新增", solution.getSolutionNo(), solution.getName());
        return ApiResult.ok(solution);
    }

    @PutMapping("/{id}")
    public ApiResult<Solution> update(@PathVariable Long id, @RequestBody Solution solution) {
        Solution existing = find(id);
        solution.setId(id);
        solution.setTenantId(existing.getTenantId());
        solution.setSolutionNo(existing.getSolutionNo());
        solution.setVersion(existing.getVersion());
        solution.setStatus(existing.getStatus());
        solution.setShareToken(existing.getShareToken());
        solution.setDemandId(existing.getDemandId());
        fillProduct(solution);
        solutionMapper.updateById(solution);
        auditService.log("产品方案", "修改", existing.getSolutionNo(), solution.getName());
        return ApiResult.ok(solutionMapper.selectById(id));
    }

    @PostMapping("/{id}/new-version")
    public ApiResult<Solution> newVersion(@PathVariable Long id) {
        var user = AuthContext.require();
        Solution existing = find(id);
        Solution copy = new Solution();
        copy.setTenantId(user.getTenantId());
        copy.setSolutionNo(idGenerator.next(user.getTenantId(), user.getTenantCode(), "SOLUTION"));
        copy.setDemandId(existing.getDemandId());
        copy.setName(existing.getName());
        copy.setVersion(solutionMapper.selectList(new LambdaQueryWrapper<Solution>()
                .eq(Solution::getDemandId, existing.getDemandId())).stream()
                .map(Solution::getVersion).filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 1);
        copy.setProductId(existing.getProductId());
        copy.setProductName(existing.getProductName());
        copy.setContent(existing.getContent());
        copy.setAttachments(existing.getAttachments());
        copy.setStatus("DRAFT");
        copy.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        copy.setViewCount(0);
        copy.setCreatedBy(user.getId());
        solutionMapper.insert(copy);
        auditService.log("产品方案", "新版本", existing.getSolutionNo(), copy.getSolutionNo());
        return ApiResult.ok(copy);
    }

    @PostMapping("/{id}/send")
    public ApiResult<Solution> send(@PathVariable Long id) {
        var user = AuthContext.require();
        Solution solution = find(id);
        if (solution.getContent() == null || solution.getContent().isBlank()) throw new BizException("方案正文不能为空");
        Demand demand = requireDemand(solution.getDemandId());
        Product product = solution.getProductId() == null ? null : productMapper.selectById(solution.getProductId());
        if (product != null && !"OK".equals(product.getComplianceStatus())) {
            throw new BizException("所选锅炉产品缺少制造许可/监检/能效资质，禁止推送方案");
        }
        solution.setStatus("PUSHED");
        solutionMapper.updateById(solution);
        String link = "/share/solution?token=" + solution.getShareToken();
        if (demand.getCustomerId() != null) {
            notificationService.notifyCustomer(user.getTenantId(), demand.getCustomerId(), "SOLUTION_PUSH",
                    "您有新方案待查看", solution.getName(), Map.of("demandNo", demand.getDemandNo(),
                            "solutionName", solution.getName(), "link", link), link);
        }
        auditService.log("产品方案", "推送", solution.getSolutionNo(), demand.getDemandNo());
        return ApiResult.ok(solution);
    }

    @PostMapping("/{id}/confirm")
    public ApiResult<Solution> confirm(@PathVariable Long id) {
        var user = AuthContext.require();
        Solution solution = find(id);
        confirmSolution(solution);
        auditService.log("产品方案", "客户确认", solution.getSolutionNo(), null);
        return ApiResult.ok(solution);
    }

    public void confirmSolution(Solution solution) {
        solution.setStatus("CONFIRMED");
        solution.setConfirmTime(java.time.LocalDateTime.now());
        solutionMapper.updateById(solution);
        Demand demand = demandMapper.selectById(solution.getDemandId());
        if (demand != null && Set.of("NEW", "CONSULTING", "INTRODUCING").contains(demand.getStatus())) {
            demand.setStatus("TALKING");
            demandMapper.updateById(demand);
        }
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Solution solution = find(id);
        if (quoteMapper.selectCount(new LambdaQueryWrapper<Quote>().eq(Quote::getSolutionId, id)) > 0) {
            throw new BizException(409, "该方案已关联报价，请先删除报价后再删除方案");
        }
        jdbcTemplate.update("DELETE FROM solution WHERE id = ?", id);
        auditService.log("产品方案", "删除", solution.getSolutionNo(), solution.getName());
        return ApiResult.ok();
    }

    private Solution find(Long id) {
        Solution solution = solutionMapper.selectById(id);
        if (solution == null) throw new BizException(404, "方案不存在");
        return solution;
    }

    private Demand requireDemand(Long demandId) {
        if (demandId == null) throw new BizException("请选择关联需求");
        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) throw new BizException(404, "关联需求不存在");
        return demand;
    }

    private void fillProduct(Solution solution) {
        if (solution.getProductId() != null) {
            Product product = productMapper.selectById(solution.getProductId());
            if (product == null) throw new BizException(404, "产品不存在");
            solution.setProductName(product.getModel());
        }
    }
}
