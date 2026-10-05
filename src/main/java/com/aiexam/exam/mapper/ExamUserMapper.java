package com.aiexam.exam.mapper;

import com.aiexam.exam.entity.ExamUser;
import com.aiexam.exam.vo.ExamStudentVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考试-考生关联 Mapper（单表 CRUD 全部由 MyBatis-Plus 提供）
 */
@Mapper
public interface ExamUserMapper extends BaseMapper<ExamUser> {

    /**
     * 查询考试下的考生列表（join sys_user 取姓名学号，SQL 在 XML 中）
     *
     * @param examId 考试ID
     * @return 考生列表
     */
    List<ExamStudentVO> selectExamStudents(@Param("examId") Long examId);
}
