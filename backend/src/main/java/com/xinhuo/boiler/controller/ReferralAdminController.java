package com.xinhuo.boiler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.entity.Referral;
import com.xinhuo.boiler.mapper.ReferralMapper;
import com.xinhuo.boiler.service.AuditService;
import com.xinhuo.boiler.service.ExcelService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/referrals")
public class ReferralAdminController extends SimpleCrudController<Referral> {
    public ReferralAdminController(ReferralMapper mapper, AuditService auditService,
                                   ExcelService excelService, ObjectMapper objectMapper) {
        super(mapper, auditService, excelService, objectMapper, "转介绍",
                List.of("target_company", "contact_name", "demand_desc"),
                List.of("status"), new LinkedHashMap<>());
    }

    @Override
    protected void beforeCreate(Referral entity) {
        if (entity.getOwnerId() == null) entity.setOwnerId(AuthContext.userId());
    }
}
