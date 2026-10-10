package com.aiexam.ai.client;

import com.aiexam.ai.config.AiGradingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议大模型客户端（RestClient 实现，零新依赖，默认智谱 GLM）
 * <p>
 * ai.grading.mock=false（或缺省）时生效。prompt 拼装收敛在本类内，
 * Service 只传判分要素数据。日志严禁打印 api-key。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.grading", name = "mock", havingValue = "false", matchIfMissing = true)
public class GlmChatClient implements AiChatClient {

    /** 无日期字段，静态共享即可 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 系统提示词：角色 + 评分规则 + 强制 JSON 输出 */
    private static final String SYSTEM_PROMPT = """
            你是一位严谨的阅卷老师，负责批改简答题。请根据题目、参考答案和评分要点，对学生答案给出分数和简短评语。
            评分要求：
            1. 以参考答案为核心依据，学生答案意思正确、要点覆盖即可给分，不要求文字与参考答案完全一致；
            2. 覆盖全部要点给满分或接近满分，覆盖部分要点按比例给分，意思错误或未作答给 0 分；
            3. 分数必须是 0 到满分之间的数字，最多两位小数，不得为负，不得超过满分；
            4. 评语不超过 100 字，指出得分或失分的原因。
            你必须只输出一个 JSON 对象，不要输出任何其他内容，格式如下：
            {"score": <分数>, "comment": "<评语>"}""";

    private final RestClient restClient;

    private final String model;

    public GlmChatClient(AiGradingProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(props.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.getReadTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
        this.model = props.getModel();
    }

    @Override
    public AiGradeResult grade(AiGradePrompt prompt) {
        try {
            Map<String, Object> request = Map.of(
                    "model", model,
                    "temperature", 0.1,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", buildUserPrompt(prompt))));
            String body = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(String.class);
            JsonNode root = OBJECT_MAPPER.readTree(body);
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            return parseGrade(content);
        } catch (Exception e) {
            log.error("大模型判分调用失败", e);
            throw new RuntimeException("大模型调用失败：" + e.getMessage());
        }
    }

    /**
     * 组装用户提示词（空值占位，避免空段落干扰模型）
     */
    private static String buildUserPrompt(AiGradePrompt prompt) {
        return """
                【题目】
                %s

                【参考答案】
                %s

                【评分要点】
                %s

                【满分】
                %s

                【学生答案】
                %s

                请按要求只输出 JSON：{"score": 分数, "comment": "评语"}""".formatted(
                blankTo(prompt.getTitle(), "（无）"),
                blankTo(prompt.getCorrectAnswer(), "（无）"),
                blankTo(prompt.getAnalysis(), "（无）"),
                prompt.getFullScore() == null ? "（无）" : prompt.getFullScore().toPlainString(),
                blankTo(prompt.getUserAnswer(), "（未作答）"));
    }

    private static String blankTo(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    /**
     * 解析大模型返回的判分 JSON（包级私有，供单测直接调用）
     * <p>
     * 用 { } 定位法截取：天然剥离 ```json 围栏与前后杂文本；score 兼容数字与字符串两种节点。
     */
    static AiGradeResult parseGrade(String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("大模型返回内容为空");
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new RuntimeException("大模型返回非 JSON 内容");
        }
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(content.substring(start, end + 1));
        } catch (Exception e) {
            throw new RuntimeException("大模型返回 JSON 解析失败");
        }
        JsonNode scoreNode = root.get("score");
        if (scoreNode == null) {
            throw new RuntimeException("大模型返回缺少 score 字段");
        }
        BigDecimal score;
        try {
            score = new BigDecimal(scoreNode.asText().trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("大模型返回 score 不是数字");
        }
        JsonNode commentNode = root.get("comment");
        String comment = commentNode == null ? null : commentNode.asText(null);
        return new AiGradeResult(score, comment);
    }
}
