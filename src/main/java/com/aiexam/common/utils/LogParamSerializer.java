package com.aiexam.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志参数序列化工具（纯静态，无 Spring 依赖，便于单测）
 * <p>
 * Servlet 对象等无关参数过滤 → 按参数名装 Map → JSON 递归脱敏（字段名含
 * password/token 一律打码）→ 超长截断（截断后可能非合法 JSON，日志场景可接受）。
 */
public final class LogParamSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** 敏感字段统一打码值 */
    private static final String MASK = "******";
    /** 截断尾标 */
    private static final String TRUNCATE_SUFFIX = "...(truncated)";

    private LogParamSerializer() {
    }

    /**
     * 序列化方法入参为脱敏 JSON；无有效参数返回 null
     *
     * @param paramNames 参数名（MethodSignature.getParameterNames()）
     * @param args       参数值
     * @param maxLength  结果最大长度
     * @return 脱敏截断后的 JSON 字符串，或 null
     */
    public static String serialize(String[] paramNames, Object[] args, int maxLength) {
        if (args == null || args.length == 0) {
            return null;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (isFiltered(args[i])) {
                continue;
            }
            String name = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
            params.put(name, args[i]);
        }
        if (params.isEmpty()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.valueToTree(params);
            mask(root);
            return truncate(root.toString(), maxLength);
        } catch (Exception e) {
            // 兜底：序列化失败不给业务添堵
            return "(参数序列化失败)";
        }
    }

    /**
     * 递归脱敏：命中敏感字段名的值打码，数组逐项下钻
     */
    static void mask(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                if (isSensitive(fieldName)) {
                    objectNode.put(fieldName, MASK);
                } else {
                    mask(objectNode.get(fieldName));
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(LogParamSerializer::mask);
        }
    }

    /**
     * 敏感字段判定：字段名小写后包含 password，或等于 token
     */
    private static boolean isSensitive(String fieldName) {
        String lower = fieldName.toLowerCase();
        return lower.contains("password") || "token".equals(lower);
    }

    /**
     * 与日志无关的参数类型过滤
     */
    private static boolean isFiltered(Object arg) {
        return arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof MultipartFile
                || arg instanceof BindingResult;
    }

    /**
     * 超长截断并加尾标
     */
    static String truncate(String json, int maxLength) {
        if (json.length() <= maxLength) {
            return json;
        }
        return json.substring(0, maxLength) + TRUNCATE_SUFFIX;
    }
}
