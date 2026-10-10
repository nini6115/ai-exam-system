package com.aiexam.ai.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模拟判分客户端单元测试（确定性规则）
 */
class MockChatClientTest {

    private static final BigDecimal FULL_SCORE = new BigDecimal("10.00");

    private final MockChatClient client = new MockChatClient();

    @Test
    @DisplayName("模拟：未作答给 0 分")
    void grade_blankAnswer_zero() {
        AiGradeResult result = client.grade(prompt("简述 JVM 内存分区", "堆 栈 方法区", null));
        assertThat(result.getScore()).isEqualByComparingTo("0");
        assertThat(result.getComment()).contains("未作答");
    }

    @Test
    @DisplayName("模拟：归一化后与参考答案一致给满分（容忍大小写/空白差异）")
    void grade_identical_fullScore() {
        AiGradeResult result = client.grade(prompt("题", "Java Virtual Machine", "java  virtual   MACHINE"));
        assertThat(result.getScore()).isEqualByComparingTo("10");
        assertThat(result.getComment()).contains("满分");
    }

    @Test
    @DisplayName("模拟：重合度≥0.6 给 60% 分")
    void grade_highOverlap_60Percent() {
        // 参考答案 6 个不同字符，学生答案覆盖 4 个 → 重合度 0.67
        AiGradeResult result = client.grade(prompt("题", "abcdef", "abcdzz"));
        assertThat(result.getScore()).isEqualByComparingTo("6.00");
        assertThat(result.getComment()).contains("60%");
    }

    @Test
    @DisplayName("模拟：重合度>0 且<0.6 给 30% 分")
    void grade_partialOverlap_30Percent() {
        AiGradeResult result = client.grade(prompt("题", "abcdef", "abzzz"));
        assertThat(result.getScore()).isEqualByComparingTo("3.00");
        assertThat(result.getComment()).contains("30%");
    }

    @Test
    @DisplayName("模拟：完全不重合给 0 分")
    void grade_noOverlap_zero() {
        AiGradeResult result = client.grade(prompt("题", "abcdef", "zzz"));
        assertThat(result.getScore()).isEqualByComparingTo("0");
        assertThat(result.getComment()).contains("不重合");
    }

    @Test
    @DisplayName("模拟：参考答案为空给 60% 分")
    void grade_blankCorrectAnswer_60Percent() {
        AiGradeResult result = client.grade(prompt("题", "  ", "随便答的"));
        assertThat(result.getScore()).isEqualByComparingTo("6.00");
        assertThat(result.getComment()).contains("无参考答案");
    }

    @Test
    @DisplayName("模拟：同输入两次调用结果完全一致（确定性）")
    void grade_deterministic() {
        AiGradePrompt prompt = prompt("题", "abcdef", "abcdzz");
        AiGradeResult first = client.grade(prompt);
        AiGradeResult second = client.grade(prompt);
        assertThat(first.getScore()).isEqualByComparingTo(second.getScore());
        assertThat(first.getComment()).isEqualTo(second.getComment());
    }

    private AiGradePrompt prompt(String title, String correctAnswer, String userAnswer) {
        return AiGradePrompt.builder()
                .title(title)
                .correctAnswer(correctAnswer)
                .analysis(null)
                .fullScore(FULL_SCORE)
                .userAnswer(userAnswer)
                .build();
    }
}
