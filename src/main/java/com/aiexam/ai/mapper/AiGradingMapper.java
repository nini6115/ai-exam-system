package com.aiexam.ai.mapper;

import com.aiexam.ai.vo.GradeDetailVO;
import com.aiexam.ai.vo.PendingGradeItemVO;
import com.aiexam.ai.vo.SubjectiveSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 判卷 Mapper（纯自定义查询，无对应实体，不继承 BaseMapper；SQL 全部在 XML）
 */
@Mapper
public interface AiGradingMapper {

    /**
     * 查某考试全部待 AI 判分的主观题明细（联表 SQL 在 XML）
     *
     * @param examId 考试ID
     * @return 待判清单（按答卷、题号排序）
     */
    List<PendingGradeItemVO> selectPendingGrades(@Param("examId") Long examId);

    /**
     * 查某答卷主观题判分进度：未判数 + 已判主观分合计（SQL 在 XML）
     *
     * @param sheetId 答卷ID
     * @return 未判数与主观分合计
     */
    SubjectiveSummaryVO selectSubjectiveSummary(@Param("sheetId") Long sheetId);

    /**
     * 教师复核：某答卷逐题判分详情（联表 question 取题型/题干，SQL 在 XML）
     *
     * @param examId  考试ID（限定 sheet 属于本场考试，越权 sheetId 返回空列表）
     * @param sheetId 答卷ID
     * @return 逐题判分详情
     */
    List<GradeDetailVO> selectSheetGradeDetails(@Param("examId") Long examId,
                                                @Param("sheetId") Long sheetId);

    /**
     * AI 判分落库（守卫 graded_by IS NULL 保证幂等，SQL 在 XML）
     *
     * @param id        明细ID
     * @param score     得分
     * @param isCorrect 是否满分：1是 0否
     * @param comment   评语
     * @param now       判分时间
     * @return 影响行数：0=该题已被其他判分（人工/并发AI）处理
     */
    int markAiGraded(@Param("id") Long id, @Param("score") BigDecimal score,
                     @Param("isCorrect") int isCorrect, @Param("comment") String comment,
                     @Param("now") LocalDateTime now);

    /**
     * 人工改分落库（comment 为 null 时不覆盖原评语，SQL 在 XML）
     *
     * @param id        明细ID
     * @param score     得分
     * @param isCorrect 是否满分：1是 0否
     * @param graderId  判分教师ID
     * @param comment   改分评语，可空
     * @param now       判分时间
     * @return 影响行数
     */
    int markManualGraded(@Param("id") Long id, @Param("score") BigDecimal score,
                         @Param("isCorrect") int isCorrect, @Param("graderId") Long graderId,
                         @Param("comment") String comment, @Param("now") LocalDateTime now);
}
