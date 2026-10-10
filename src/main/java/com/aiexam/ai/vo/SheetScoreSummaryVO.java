package com.aiexam.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 答卷成绩汇总VO（主观题判完/人工改分后返回）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SheetScoreSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 答卷ID */
    private Long sheetId;

    /** 客观题得分 */
    private BigDecimal objectiveScore;

    /** 主观题得分（整卷仍有待判主观题时为 null，表示成绩未定稿） */
    private BigDecimal subjectiveScore;

    /** 总分 = 客观 + 主观 */
    private BigDecimal totalScore;
}
