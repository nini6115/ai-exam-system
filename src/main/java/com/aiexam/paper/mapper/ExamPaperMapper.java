package com.aiexam.paper.mapper;

import com.aiexam.paper.entity.ExamPaper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 试卷 Mapper（单表 CRUD 全部由 MyBatis-Plus 提供）
 */
@Mapper
public interface ExamPaperMapper extends BaseMapper<ExamPaper> {
}
