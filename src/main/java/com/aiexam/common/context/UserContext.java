package com.aiexam.common.context;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前用户
     */
    public static void set(LoginUser user) {
        THREAD_LOCAL.set(user);
    }

    /**
     * 获取当前用户
     */
    public static LoginUser get() {
        return THREAD_LOCAL.get();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        LoginUser user = THREAD_LOCAL.get();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        LoginUser user = THREAD_LOCAL.get();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 清理（必须在请求结束时调用，防止内存泄漏）
     */
    public static void clear() {
        THREAD_LOCAL.remove();
    }
}
