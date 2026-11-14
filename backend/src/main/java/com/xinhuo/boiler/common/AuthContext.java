package com.xinhuo.boiler.common;

public final class AuthContext {
    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static AuthUser get() {
        return HOLDER.get();
    }

    public static AuthUser require() {
        AuthUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(401, "登录已失效，请重新登录");
        }
        return user;
    }

    public static Long tenantId() {
        return require().getTenantId();
    }

    public static Long userId() {
        return require().getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
