package com.aiexam.analysis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 考试统计概览VO
 * <p>
 * attendedCount/passedCount/avgScore/maxScore/minScore 由统计 SQL 直接填充（见 AnalysisMapper.xml），
 * 其余字段由 Service 组装。
 */
@Data
public class ExamStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考试ID */
    private Long examId;

    /** 考试名称 */
    private String examName;

    /** 卷面总分 */
    private BigDecimal totalScore;

    /** 及格分 */
    private BigDecimal passScore;

    /** 应考人数（名单人数） */
    private Long assignedCount;

    /** 参考人数（已交卷人数，按每人最新一张已交卷答卷计） */
    private Long attendedCount;

    /** 缺考人数 = 应考 - 参考 */
    private Long absentCount;

    /** 及格人数 */
    private Long passedCount;

    /** 平均分（2位小数，无人交卷为 null） */
    private BigDecimal avgScore;

    /** 最高分（无人交卷为 null） */
    private BigDecimal maxScore;

    /** 最低分（无人交卷为 null） */
    private BigDecimal minScore;

    /** 及格率（百分数，2位小数，如 85.50 表示 85.5%；无人交卷为 0） */
    private BigDecimal passRate;

    /** 分数段分布（恒定5段，空段计数为0） */
    private List<ScoreSegmentVO> scoreSegments;
}
