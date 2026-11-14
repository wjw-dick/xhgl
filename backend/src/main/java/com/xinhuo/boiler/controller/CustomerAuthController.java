package com.xinhuo.boiler.controller;

import com.xinhuo.boiler.common.ApiResult;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.entity.Customer;
import com.xinhuo.boiler.mapper.CustomerMapper;
import com.xinhuo.boiler.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/auth")
@RequiredArgsConstructor
public class CustomerAuthController {
    private final AuthService authService;
    private final CustomerMapper customerMapper;

    @Data
    public static class CustomerLogin {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody CustomerLogin request) {
        return ApiResult.ok(authService.loginCustomer(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        var user = AuthContext.require();
        Customer customer = customerMapper.selectById(user.getCustomerId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("customer", customer);
        return ApiResult.ok(result);
    }
}
