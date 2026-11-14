package com.xinhuo.boiler.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinhuo.boiler.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.xinhuo.boiler.common.Audit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Audit audit = signature.getMethod().getAnnotation(Audit.class);
        String detail;
        try {
            detail = objectMapper.writeValueAsString(Arrays.stream(joinPoint.getArgs())
                    .filter(a -> a != null && !(a instanceof jakarta.servlet.http.HttpServletRequest)
                            && !(a instanceof jakarta.servlet.http.HttpServletResponse))
                    .toList());
        } catch (Exception e) {
            detail = "params";
        }
        try {
            Object result = joinPoint.proceed();
            auditService.log(audit.module(), audit.action(), signature.getName(), detail);
            return result;
        } catch (Throwable t) {
            auditService.log(audit.module(), audit.action() + ":FAIL", signature.getName(),
                    "错误=" + t.getMessage() + "; 参数=" + detail);
            throw t;
        }
    }
}
