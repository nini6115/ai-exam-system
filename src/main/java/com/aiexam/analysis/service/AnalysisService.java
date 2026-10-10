package com.aiexam.analysis.service;

import com.aiexam.analysis.dto.ScoreQueryDTO;
import com.aiexam.analysis.vo.ExamStatsVO;
import com.aiexam.analysis.vo.ScoreExportVO;
import com.aiexam.analysis.vo.ScoreItemVO;
import com.aiexam.common.vo.PageVO;

/**
 * 成绩分析服务（考试统计 / 成绩明细 / CSV 导出）
 */
public interface AnalysisService {

    /**
     * 考试统计概览：应考/参考/缺考人数、均分极值、及格率、分数段分布
     * <p>
     * 统计口径：每位考生取最新一张已交卷答卷（status IN 2,3,4）。
     *
     * @param examId 考试ID
     * @return 统计结果
     */
    ExamStatsVO getExamStats(Long examId);

    /**
     * 成绩明细分页：本场考试全名单（未考考生也返回，答卷字段为 null）
     *
     * @param examId 考试ID
     * @param dto    分页与搜索参数
     * @return 成绩明细分页
     */
    PageVO<ScoreItemVO> getScorePage(Long examId, ScoreQueryDTO dto);

    /**
     * 成绩明细 CSV 导出（与明细同一条查询、不分页全量导出）
     *
     * @param examId  考试ID
     * @param keyword 姓名/学号模糊搜索，可空
     * @return 文件名 + CSV 内容（UTF-8 带 BOM）
     */
    ScoreExportVO exportScores(Long examId, String keyword);
}
