package com.aiexam.common.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局时间格式：LocalDateTime 统一为 yyyy-MM-dd HH:mm:ss。
 * <p>
 * 默认 ISO 格式带 T（2026-10-17T15:00:00），前端 Element Plus 传的是空格分隔，
 * 不配置则所有 LocalDateTime 入参 DTO 反序列化报错。字段上的 @JsonFormat 优先级更高，出参不受影响。
 */
@Configuration
public class JacksonConfig {

    /** 全局时间格式 */
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(DATETIME_FORMAT))
                .deserializers(new LocalDateTimeDeserializer(DATETIME_FORMAT));
    }
}
