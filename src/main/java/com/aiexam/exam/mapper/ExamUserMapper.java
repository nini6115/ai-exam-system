package com.aiexam.exam.mapper;

import com.aiexam.exam.entity.ExamUser;
import com.aiexam.exam.vo.ExamHallVO;
import com.aiexam.exam.vo.ExamStudentVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
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

    /**
     * 学生考试大厅：我被安排的考试分页（join exam/试卷/最新答卷，状态按时间实时推导，SQL 在 XML 中）
     *
     * @param page   分页对象
     * @param userId 当前学生ID
     * @param status 状态筛选：1待开始 2进行中 3已结束，null=全部
     * @return 考试分页
     */
    IPage<ExamHallVO> selectMyExamPage(Page<ExamHallVO> page, @Param("userId") Long userId,
                                       @Param("status") Integer status);

    /**
     * 原子累加已考次数（带次数上限守卫，SQL 在 XML 中）
     *
     * @param examId      考试ID
     * @param userId      考生ID
     * @param maxAttempts 允许次数
     * @return 影响行数：0=次数已用尽
     */
    int increaseAttempts(@Param("examId") Long examId, @Param("userId") Long userId,
                         @Param("maxAttempts") int maxAttempts);

    /**
     * 交卷后刷新最高分（GREATEST 原子比较，SQL 在 XML 中）
     *
     * @param examId 考试ID
     * @param userId 考生ID
     * @param score  本次得分
     * @return 影响行数
     */
    int updateBestScore(@Param("examId") Long examId, @Param("userId") Long userId,
                        @Param("score") BigDecimal score);
}
