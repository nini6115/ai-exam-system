package com.aiexam.question.mapper;

import com.aiexam.question.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 题目 Mapper（单表 CRUD 全部由 MyBatis-Plus 提供）
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 按题型/难度/分类随机抽取指定数量的题目ID（组卷用，SQL 在 XML 中）
     *
     * @param excludeIds 已选中的题目ID，避免多条抽题配置抽到同一道题
     * @return 随机选出的题目ID，数量可能小于 limit（题库不足时）
     */
    List<Long> selectRandomIds(@Param("type") Integer type,
                               @Param("difficulty") Integer difficulty,
                               @Param("category") String category,
                               @Param("excludeIds") List<Long> excludeIds,
                               @Param("limit") Integer limit);
}
