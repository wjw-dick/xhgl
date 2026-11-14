package com.xinhuo.boiler.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    private Long id;
    private String username;
    private String name;
    private String userType;
    @JsonIgnore
    private Long tenantId;
    @JsonIgnore
    private String tenantCode;
    @JsonIgnore
    private String tenantName;
    private String roleCode;
    private Long customerId;
    private String permissions;
    private boolean readOnly;

    public boolean isCustomer() {
        return "CUSTOMER".equals(userType);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(roleCode);
    }
}
