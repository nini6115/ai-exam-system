package com.aiexam.ai.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 答卷主观题判分进度VO（mapper 聚合结果）
 */
@Data
public class SubjectiveSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 未判主观题数 */
    private Long ungradedCount;

    /** 已判主观分合计（SUM 忽略 NULL，空集兜底 0） */
    private BigDecimal subjectiveScore;
}
