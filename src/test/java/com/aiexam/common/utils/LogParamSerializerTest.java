package com.aiexam.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 操作日志参数序列化工具单元测试（纯函数）
 */
class LogParamSerializerTest {

    private static final int MAX_LENGTH = 2000;

    @Test
    @DisplayName("序列化：单参数输出 JSON")
    void serialize_singleParam() {
        String json = LogParamSerializer.serialize(new String[]{"id"}, new Object[]{5L}, MAX_LENGTH);
        assertThat(json).isEqualTo("{\"id\":5}");
    }

    @Test
    @DisplayName("序列化：多参数按声明顺序输出")
    void serialize_multipleParams_keepOrder() {
        String json = LogParamSerializer.serialize(
                new String[]{"username", "status"}, new Object[]{"tom", 1}, MAX_LENGTH);
        assertThat(json).isEqualTo("{\"username\":\"tom\",\"status\":1}");
    }

    @Test
    @DisplayName("序列化：无参数或全部被过滤时返回 null")
    void serialize_empty_returnsNull() {
        assertThat(LogParamSerializer.serialize(new String[0], new Object[0], MAX_LENGTH)).isNull();
        assertThat(LogParamSerializer.serialize(null, null, MAX_LENGTH)).isNull();
        HttpServletRequest request = new MockHttpServletRequest();
        assertThat(LogParamSerializer.serialize(new String[]{"request"},
                new Object[]{request}, MAX_LENGTH)).isNull();
    }

    @Test
    @DisplayName("脱敏：password/newPassword/oldPassword/token 一律打码，其余原样")
    void serialize_masksSensitiveFields() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "tom");
        body.put("password", "secret123");
        body.put("newPassword", "new456");
        body.put("oldPassword", "old789");
        body.put("token", "tok-xyz");
        body.put("realName", "汤姆");

        String json = LogParamSerializer.serialize(new String[]{"dto"}, new Object[]{body}, MAX_LENGTH);

        assertThat(json).contains("\"password\":\"******\"");
        assertThat(json).contains("\"newPassword\":\"******\"");
        assertThat(json).contains("\"oldPassword\":\"******\"");
        assertThat(json).contains("\"token\":\"******\"");
        assertThat(json).contains("\"realName\":\"汤姆\"");
        assertThat(json).doesNotContain("secret123").doesNotContain("new456").doesNotContain("tok-xyz");
    }

    @Test
    @DisplayName("脱敏：嵌套对象同样打码")
    void serialize_masksNestedFields() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("password", "inner-secret");
        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("body", nested);

        String json = LogParamSerializer.serialize(new String[]{"dto"}, new Object[]{outer}, MAX_LENGTH);

        assertThat(json).contains("\"password\":\"******\"");
        assertThat(json).doesNotContain("inner-secret");
    }

    @Test
    @DisplayName("截断：超长参数截断到上限并加尾标")
    void serialize_truncates() {
        String longText = "字".repeat(3000);
        String json = LogParamSerializer.serialize(new String[]{"content"},
                new Object[]{longText}, MAX_LENGTH);

        assertThat(json.length()).isLessThanOrEqualTo(MAX_LENGTH + "...(truncated)".length());
        assertThat(json).endsWith("...(truncated)");
    }
}
