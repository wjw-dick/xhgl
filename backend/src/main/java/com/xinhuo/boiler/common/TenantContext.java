package com.xinhuo.boiler.common;

/**
 * 企业数据域上下文：JWT 解析后保存于 ThreadLocal，供 MyBatis-Plus 数据域插件读取。
 * 单企业模式下固定为系统企业数据域，不再支持跨企业切换。
 */
public final class TenantContext {
    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static AuthUser get() {
        return HOLDER.get();
    }

    public static Long getTenantId() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.getTenantId();
    }

    public static boolean isIgnore() {
        AuthUser user = HOLDER.get();
        return user == null || user.getTenantId() == null;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
