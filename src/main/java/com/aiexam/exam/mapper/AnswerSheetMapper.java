package com.aiexam.exam.mapper;

import com.aiexam.exam.entity.AnswerSheet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 答卷 Mapper（单表 CRUD 全部由 MyBatis-Plus 提供）
 */
@Mapper
public interface AnswerSheetMapper extends BaseMapper<AnswerSheet> {
}
