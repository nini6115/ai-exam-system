package com.aiexam.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 判卷配置（ai.grading 前缀）
 * <p>
 * 全字段带默认值：application.yml 缺失也能启动，调用时才报错。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.grading")
public class AiGradingProperties {

    /** true=启用内置模拟判分（无需 API Key，按答案重合度确定性给分） */
    private boolean mock = false;

    /** OpenAI 兼容服务地址（默认智谱 GLM，可换任何兼容服务） */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    /** API Key（mock=true 时无需配置；严禁打日志） */
    private String apiKey = "";

    /** 模型名 */
    private String model = "glm-4-flash";

    /** 建连超时 */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** 读取超时（LLM 生成较慢，默认放宽到 60 秒） */
    private Duration readTimeout = Duration.ofSeconds(60);
}
