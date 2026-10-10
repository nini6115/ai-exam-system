package com.aiexam.ai.client;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 判分要素（Service 只传数据，prompt 文本由客户端实现自行拼装）
 */
@Data
@Builder
public class AiGradePrompt {

    /** 题干 */
    private String title;

    /** 参考答案 */
    private String correctAnswer;

    /** 评分要点（题目解析） */
    private String analysis;

    /** 本题满分 */
    private BigDecimal fullScore;

    /** 学生答案 */
    private String userAnswer;
}
