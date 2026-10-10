package com.aiexam.analysis.mapper;

import com.aiexam.analysis.vo.ExamStatsVO;
import com.aiexam.analysis.vo.ScoreItemVO;
import com.aiexam.analysis.vo.ScoreSegmentVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成绩分析 Mapper（纯自定义统计查询，无对应实体，不继承 BaseMapper；SQL 全部在 XML）
 */
@Mapper
public interface AnalysisMapper {

    /**
     * 已交卷答卷统计汇总（参考/及格人数、均分极值；无数据时聚合列为 NULL，Service 兜底）
     *
     * @param examId 考试ID
     * @return 汇总行（仅填充 attendedCount/passedCount/avgScore/maxScore/minScore）
     */
    ExamStatsVO selectScoreSummary(@Param("examId") Long examId);

    /**
     * 分数段分布（按得分率 5 段，只返回非空段，Service 补齐空段）
     *
     * @param examId 考试ID
     * @return 非空段列表（level 1~5 升序）
     */
    List<ScoreSegmentVO> selectScoreSegments(@Param("examId") Long examId);

    /**
     * 成绩明细分页（exam_user 全名单 LEFT JOIN 最新已交卷答卷，未考考生答卷字段为 NULL）
     *
     * @param page    分页对象（导出场景传 size<0 的 Page 表示不分页）
     * @param examId  考试ID
     * @param keyword 姓名/学号模糊，null=全部
     * @return 成绩明细分页
     */
    IPage<ScoreItemVO> selectScorePage(Page<ScoreItemVO> page, @Param("examId") Long examId,
                                       @Param("keyword") String keyword);

    /**
     * is_passed 惰性回填：仅已交卷答卷，按试卷及格分判定
     *
     * @param examId 考试ID
     * @return 影响行数
     */
    int fillIsPassed(@Param("examId") Long examId);
}
