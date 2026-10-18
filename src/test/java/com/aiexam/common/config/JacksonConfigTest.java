package com.aiexam.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证全局 LocalDateTime 格式 yyyy-MM-dd HH:mm:ss 的序列化与反序列化
 */
class JacksonConfigTest {

    /** 与 Spring Boot 相同的 builder 方式构建，再套上自定义 customizer */
    private ObjectMapper mapperWithCustomizer() {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfig().localDateTimeCustomizer().customize(builder);
        return builder.build();
    }

    @Test
    void deserializeFrontendDatetimeString() throws Exception {
        LocalDateTime time = mapperWithCustomizer()
                .readValue("\"2026-10-17 15:00:00\"", LocalDateTime.class);
        assertEquals(LocalDateTime.of(2026, 10, 17, 15, 0, 0), time);
    }

    @Test
    void serializeWithoutIsoT() throws Exception {
        String json = mapperWithCustomizer()
                .writeValueAsString(LocalDateTime.of(2026, 10, 17, 15, 0, 0));
        assertEquals("\"2026-10-17 15:00:00\"", json);
    }
}
