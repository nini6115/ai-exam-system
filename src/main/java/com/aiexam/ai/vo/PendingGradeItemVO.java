package com.aiexam.ai.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 待判主观题明细VO（内部驱动判分，联表查询结果）
 */
@Data
public class PendingGradeItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 答题明细ID */
    private Long detailId;

    /** 答卷ID */
    private Long sheetId;

    /** 题目ID */
    private Long questionId;

    /** 答卷中的题号 */
    private Integer sortOrder;

    /** 题干 */
    private String title;

    /** 学生答案 */
    private String userAnswer;

    /** 参考答案（交卷时冗余快照） */
    private String correctAnswer;

    /** 评分要点（题目解析） */
    private String analysis;

    /** 本题满分（交卷时冗余快照） */
    private BigDecimal fullScore;
}
