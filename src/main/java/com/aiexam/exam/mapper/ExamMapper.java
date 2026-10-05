package com.aiexam.exam.mapper;

import com.aiexam.exam.entity.Exam;
import com.aiexam.exam.vo.ExamVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 考试发布 Mapper（单表 CRUD 全部由 MyBatis-Plus 提供）
 */
@Mapper
public interface ExamMapper extends BaseMapper<Exam> {

    /**
     * 分页查询考试列表（join 试卷取名、子查询统计人数、实时推导状态，SQL 在 XML 中）
     *
     * @param page 分页参数
     * @param name 考试名称，模糊查询，可为空
     * @return 考试列表分页
     */
    IPage<ExamVO> selectExamPage(Page<ExamVO> page, @Param("name") String name);
}
