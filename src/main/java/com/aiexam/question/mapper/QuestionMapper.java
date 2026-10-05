package com.aiexam.question.mapper;

import com.aiexam.question.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目 Mapper（单表 CRUD 全部由 MyBatis-Plus 提供）
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
