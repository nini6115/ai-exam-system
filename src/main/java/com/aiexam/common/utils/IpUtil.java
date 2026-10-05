package com.aiexam.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端IP工具
 */
public class IpUtil {

    /**
     * 从请求头提取客户端真实IP（X-Forwarded-For → X-Real-IP → getRemoteAddr）
     */
    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
