package com.aiexam.paper.mapper;

import com.aiexam.paper.entity.ExamPaperQuestion;
import com.aiexam.paper.vo.PaperQuestionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 试卷-题目关联 Mapper（单表 CRUD 全部由 MyBatis-Plus 提供）
 */
@Mapper
public interface ExamPaperQuestionMapper extends BaseMapper<ExamPaperQuestion> {

    /**
     * 查询试卷下的题目列表（join question，一次查询，不含答案，SQL 在 XML 中）
     */
    List<PaperQuestionVO> selectPaperQuestions(@Param("paperId") Long paperId);
}
