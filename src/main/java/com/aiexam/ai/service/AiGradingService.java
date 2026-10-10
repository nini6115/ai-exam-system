package com.aiexam.ai.service;

import com.aiexam.ai.dto.ManualGradeDTO;
import com.aiexam.ai.vo.GradeBatchResultVO;
import com.aiexam.ai.vo.GradeDetailVO;
import com.aiexam.ai.vo.SheetScoreSummaryVO;

import java.util.List;

/**
 * AI 判卷服务（批量判卷 / 复核视图 / 人工改分 / 单卷成绩汇总）
 */
public interface AiGradingService {

    /**
     * 教师手动触发整场考试批量 AI 判卷：串行逐题判分，单题失败不阻断批次，
     * 涉及答卷在主观题全部判完后自动汇总成绩
     *
     * @param examId 考试ID
     * @return 待判/成功/失败统计与失败明细
     */
    GradeBatchResultVO gradeExam(Long examId);

    /**
     * 教师复核：某答卷逐题判分详情（含客观题，整卷呈现）
     *
     * @param examId  考试ID
     * @param sheetId 答卷ID（不属于本场考试时返回空列表）
     * @return 逐题判分详情
     */
    List<GradeDetailVO> getSheetGradeDetails(Long examId, Long sheetId);

    /**
     * 人工改分一道主观题（graded_by=2）并尝试汇总该卷
     *
     * @param detailId 答题明细ID
     * @param dto      分数与可选评语
     * @return 该卷最新成绩汇总（卷未判完时 subjectiveScore 为 null）
     */
    SheetScoreSummaryVO manualGrade(Long detailId, ManualGradeDTO dto);
}
