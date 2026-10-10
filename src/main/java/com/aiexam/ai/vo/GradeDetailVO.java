package com.aiexam.ai.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 答卷逐题判分详情VO（教师复核视图，含客观题整卷呈现）
 */
@Data
public class GradeDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 答题明细ID（人工改分 PUT 必需） */
    private Long detailId;

    /** 题目ID */
    private Long questionId;

    /** 答卷中的题号 */
    private Integer sortOrder;

    /** 题型：1单选 2多选 3判断 4填空 5简答 */
    private Integer type;

    /** 题干 */
    private String title;

    /** 学生答案 */
    private String userAnswer;

    /** 参考答案 */
    private String correctAnswer;

    /** 本题得分（主观题未判为 null） */
    private BigDecimal score;

    /** 本题满分 */
    private BigDecimal fullScore;

    /** 是否正确：1是 0否（主观题未判为 null） */
    private Integer isCorrect;

    /** 判分方式：1系统 2人工 3 AI（未判为 null） */
    private Integer gradedBy;

    /** 判分评语 */
    private String gradeComment;
}
