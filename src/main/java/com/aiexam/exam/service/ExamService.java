package com.aiexam.exam.service;

import com.aiexam.common.vo.PageVO;
import com.aiexam.exam.dto.ExamHallQueryDTO;
import com.aiexam.exam.dto.ExamPublishDTO;
import com.aiexam.exam.dto.ExamQueryDTO;
import com.aiexam.exam.vo.ExamDetailVO;
import com.aiexam.exam.vo.ExamHallVO;
import com.aiexam.exam.vo.ExamVO;

/**
 * 考试发布服务
 */
public interface ExamService {

    /**
     * 发布考试：写入 exam + exam_user
     *
     * @param dto 发布参数
     * @return 考试ID
     */
    Long publish(ExamPublishDTO dto);

    /**
     * 分页查询考试列表（教师端）
     *
     * @param dto 查询参数
     * @return 考试分页
     */
    PageVO<ExamVO> listExams(ExamQueryDTO dto);

    /**
     * 考试详情（含试卷摘要与学生列表）
     *
     * @param id 考试ID
     * @return 考试详情
     */
    ExamDetailVO getExamDetail(Long id);

    /**
     * 学生考试大厅：我被安排的考试分页（状态按时间实时推导）
     *
     * @param dto 查询参数（状态筛选可选）
     * @return 考试分页
     */
    PageVO<ExamHallVO> getMyExams(ExamHallQueryDTO dto);
}
