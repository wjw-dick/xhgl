package com.xinhuo.boiler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinhuo.boiler.common.BizException;
import com.xinhuo.boiler.entity.Contact;
import com.xinhuo.boiler.entity.Customer;
import com.xinhuo.boiler.entity.Tenant;
import com.xinhuo.boiler.entity.TenantUser;
import com.xinhuo.boiler.mapper.ContactMapper;
import com.xinhuo.boiler.mapper.CustomerMapper;
import com.xinhuo.boiler.mapper.TenantUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final TenantUserMapper tenantUserMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final IdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final CryptoService cryptoService;
    private final AuditService auditService;
    private final AuthService authService;

    @Transactional
    public Map<String, Object> registerCustomer(Map<String, String> body) {
        Tenant tenant = authService.requireCompany();
        String contactName = required(body, "contactName", "请输入联系人姓名");
        if (contactName.length() > 32) throw new BizException("联系人姓名不能超过 32 个字符");
        String companyInput = text(body.get("companyName"), 100, null);
        String companyName = companyInput == null || companyInput.isBlank() ? contactName : companyInput;
        String phone = phone(body.get("phone"));
        String username = username(body.get("username"));
        String password = password(body.get("password"));
        String email = email(body.get("email"));
        String industry = text(body.get("industry"), 64, null);
        String region = text(body.get("region"), 64, null);

        if (tenantUserMapper.selectCount(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tenant.getId()).eq(TenantUser::getUsername, username)) > 0) {
            throw new BizException(409, "该登录账号已被使用");
        }
        if (tenantUserMapper.selectCount(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tenant.getId()).eq(TenantUser::getRoleCode, "CUSTOMER")
                .eq(TenantUser::getPhone, phone)) > 0) {
            throw new BizException(409, "该手机号已注册，请直接登录");
        }
        long userCount = tenantUserMapper.selectCount(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tenant.getId()));
        if (tenant.getMaxUsers() != null && userCount >= tenant.getMaxUsers()) {
            throw new BizException(409, "该企业账号名额已满，请联系企业管理员");
        }
        long customerCount = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantId, tenant.getId()));
        if (tenant.getMaxCustomers() != null && customerCount >= tenant.getMaxCustomers()) {
            throw new BizException(409, "该企业客户档案数量已达上限");
        }

        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setCustomerNo(idGenerator.next(tenant.getId(), tenant.getCode(), "CUSTOMER"));
        customer.setName(companyName);
        customer.setIndustry(industry);
        customer.setRegion(region);
        customer.setSource("REGISTER");
        customer.setCategory("线上注册");
        customer.setLevel("C");
        customer.setStatus("NORMAL");
        customer.setRemark("客户自助注册");
        customerMapper.insert(customer);

        Contact contact = new Contact();
        contact.setTenantId(tenant.getId());
        contact.setCustomerId(customer.getId());
        contact.setName(contactName);
        contact.setTitle("联系人");
        contact.setPhoneEnc(cryptoService.encrypt(phone));
        contact.setPhoneMask(cryptoService.maskPhone(phone));
        contact.setEmail(email);
        contact.setPrimaryFlag(1);
        contactMapper.insert(contact);

        TenantUser user = new TenantUser();
        user.setTenantId(tenant.getId());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(contactName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRoleCode("CUSTOMER");
        user.setCustomerId(customer.getId());
        user.setStatus("NORMAL");
        user.setRemark("客户自助注册");
        tenantUserMapper.insert(user);

        auditService.logForTenant(tenant.getId(), "客户注册", "开通用户账号", username,
                username, "客户：" + companyName + "，联系人：" + contactName);
        return authService.buildSession(tenant, user, customer);
    }

    private String required(Map<String, String> body, String key, String message) {
        String value = body.get(key) == null ? "" : body.get(key).trim();
        if (value.isBlank()) throw new BizException(message);
        return value;
    }

    private String text(String value, int max, String fallback) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) return fallback;
        if (text.length() > max) throw new BizException("输入内容过长");
        return text;
    }

    private String username(String value) {
        String username = value == null ? "" : value.trim();
        if (username.isBlank()) throw new BizException("请输入登录账号");
        if (!username.matches("[A-Za-z0-9_]{4,32}")) {
            throw new BizException("登录账号须为 4-32 位字母、数字或下划线");
        }
        return username;
    }

    private String password(String value) {
        String password = value == null ? "" : value;
        if (password.length() < 8 || password.length() > 64) throw new BizException("密码长度须为 8-64 位");
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            throw new BizException("密码须同时包含字母和数字");
        }
        return password;
    }

    private String phone(String value) {
        String phone = value == null ? "" : value.trim();
        if (phone.isBlank()) throw new BizException("请输入手机号");
        if (!phone.matches("1[3-9][0-9]{9}")) throw new BizException("请输入正确的 11 位手机号");
        return phone;
    }

    private String email(String value) {
        String email = value == null ? "" : value.trim();
        if (email.isBlank()) return "";
        if (email.length() > 120 || !email.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")) {
            throw new BizException("请输入正确的邮箱地址");
        }
        return email;
    }
}
