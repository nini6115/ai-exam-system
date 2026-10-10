package com.aiexam.ai.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 大模型返回内容解析单元测试（纯解析逻辑，不发 HTTP）
 */
class GlmChatClientTest {

    @Test
    @DisplayName("解析：标准 JSON")
    void parse_plainJson() {
        AiGradeResult result = GlmChatClient.parseGrade("{\"score\": 8, \"comment\": \"好\"}");
        assertThat(result.getScore()).isEqualByComparingTo("8");
        assertThat(result.getComment()).isEqualTo("好");
    }

    @Test
    @DisplayName("解析：剥离 markdown 代码围栏")
    void parse_stripsCodeFence() {
        String content = "```json\n{\"score\": 7.5, \"comment\": \"基本正确\"}\n```";
        AiGradeResult result = GlmChatClient.parseGrade(content);
        assertThat(result.getScore()).isEqualByComparingTo("7.5");
        assertThat(result.getComment()).isEqualTo("基本正确");
    }

    @Test
    @DisplayName("解析：容忍前后杂文本")
    void parse_toleratesSurroundingText() {
        String content = "好的，以下是判分结果：{\"score\": 6, \"comment\": \"覆盖一半要点\"} 希望有帮助";
        AiGradeResult result = GlmChatClient.parseGrade(content);
        assertThat(result.getScore()).isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("解析：字符串数字兼容")
    void parse_stringNumber() {
        AiGradeResult result = GlmChatClient.parseGrade("{\"score\": \"7.5\", \"comment\": \"ok\"}");
        assertThat(result.getScore()).isEqualByComparingTo("7.5");
    }

    @Test
    @DisplayName("解析：缺 score 字段应报错")
    void parse_missingScore_throws() {
        assertThatThrownBy(() -> GlmChatClient.parseGrade("{\"comment\": \"…\"}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("score");
    }

    @Test
    @DisplayName("解析：无花括号的纯文本应报错")
    void parse_noJson_throws() {
        assertThatThrownBy(() -> GlmChatClient.parseGrade("抱歉我无法判分"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("非 JSON");
    }

    @Test
    @DisplayName("解析：损坏 JSON / 空串 / null 均报错")
    void parse_brokenAndBlank_throws() {
        assertThatThrownBy(() -> GlmChatClient.parseGrade("{broken"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> GlmChatClient.parseGrade("   "))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("为空");
        assertThatThrownBy(() -> GlmChatClient.parseGrade(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("为空");
    }

    @Test
    @DisplayName("解析：缺 comment 字段时评语为 null")
    void parse_missingComment_null() {
        AiGradeResult result = GlmChatClient.parseGrade("{\"score\": 8}");
        assertThat(result.getScore()).isEqualByComparingTo("8");
        assertThat(result.getComment()).isNull();
    }
}
