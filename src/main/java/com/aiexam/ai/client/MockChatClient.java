package com.aiexam.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模拟判分客户端（ai.grading.mock=true 时生效）
 * <p>
 * 按学生答案与参考答案的字符重合度确定性给分，无需 API Key 即可演示/联调完整判分-汇总链路。
 * 规则：未作答→0；无参考答案→60%；归一化（去空白转小写）后一致→满分；
 * 重合度≥0.6→60%、>0→30%、否则 0 分。同输入恒同输出。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.grading", name = "mock", havingValue = "true")
public class MockChatClient implements AiChatClient {

    private static final String COMMENT_PREFIX = "【模拟判分】";
    private static final BigDecimal HIGH_OVERLAP_RATIO = new BigDecimal("0.6");
    private static final BigDecimal HIGH_SCORE_RATE = new BigDecimal("0.6");
    private static final BigDecimal LOW_SCORE_RATE = new BigDecimal("0.3");
    private static final BigDecimal HUNDRED = new BigDecimal(100);

    @Override
    public AiGradeResult grade(AiGradePrompt prompt) {
        BigDecimal fullScore = prompt.getFullScore() == null ? BigDecimal.ZERO : prompt.getFullScore();

        // 未作答：0 分
        if (prompt.getUserAnswer() == null || prompt.getUserAnswer().isBlank()) {
            return new AiGradeResult(scale(BigDecimal.ZERO), COMMENT_PREFIX + "未作答，不得分。");
        }
        // 无参考答案：按惯例给 60%
        if (prompt.getCorrectAnswer() == null || prompt.getCorrectAnswer().isBlank()) {
            return new AiGradeResult(rate(fullScore, HIGH_SCORE_RATE), COMMENT_PREFIX + "无参考答案，按惯例给 60% 分。");
        }

        String user = normalize(prompt.getUserAnswer());
        String correct = normalize(prompt.getCorrectAnswer());
        if (!user.isEmpty() && user.equals(correct)) {
            return new AiGradeResult(scale(fullScore), COMMENT_PREFIX + "答案与参考答案一致，给满分。");
        }

        // 字符集重合度 = 归一化后学生答案与参考答案的字符交集 / 参考答案字符数
        Set<Integer> correctChars = correct.chars().boxed().collect(Collectors.toSet());
        Set<Integer> userChars = user.chars().boxed().collect(Collectors.toSet());
        long overlapCount = correctChars.stream().filter(userChars::contains).count();
        int overlapPercent = new BigDecimal(overlapCount).multiply(HUNDRED)
                .divide(new BigDecimal(correctChars.size()), 0, RoundingMode.HALF_UP).intValue();

        BigDecimal overlap = new BigDecimal(overlapCount)
                .divide(new BigDecimal(correctChars.size()), 4, RoundingMode.HALF_UP);
        if (overlap.compareTo(HIGH_OVERLAP_RATIO) >= 0) {
            return new AiGradeResult(rate(fullScore, HIGH_SCORE_RATE),
                    COMMENT_PREFIX + "答案重合度较高（" + overlapPercent + "%），给 60% 分。");
        }
        if (overlapCount > 0) {
            return new AiGradeResult(rate(fullScore, LOW_SCORE_RATE),
                    COMMENT_PREFIX + "答案部分重合（" + overlapPercent + "%），给 30% 分。");
        }
        return new AiGradeResult(scale(BigDecimal.ZERO), COMMENT_PREFIX + "答案与参考答案不重合，不给分。");
    }

    /**
     * 归一化：去除所有空白并转小写
     */
    private static String normalize(String value) {
        return value.replaceAll("\\s+", "").toLowerCase();
    }

    private static BigDecimal rate(BigDecimal fullScore, BigDecimal rate) {
        return scale(fullScore.multiply(rate));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
