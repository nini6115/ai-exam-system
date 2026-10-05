package com.aiexam.exam.service;

import com.aiexam.exam.dto.AnswerSaveDTO;
import com.aiexam.exam.vo.ExamStartVO;

/**
 * 学生答题服务（进入考试 / 草稿保存）
 */
public interface AnswerSheetService {

    /**
     * 学生进入考试：校验名单与时间、创建（或复用）答卷、返回乱序题目
     *
     * @param examId    考试ID
     * @param ip        学生IP
     * @param userAgent 学生浏览器UA
     * @return 考试信息 + 题目列表（不含答案）
     */
    ExamStartVO start(Long examId, String ip, String userAgent);

    /**
     * 定时保存单题草稿（只写 Redis，交卷时统一落库）
     *
     * @param examId 考试ID
     * @param dto    题目ID + 学生答案
     */
    void saveAnswer(Long examId, AnswerSaveDTO dto);

    /**
     * 学生交卷：草稿落库、客观题自动判分、更新答卷状态、清空 Redis 草稿
     *
     * @param examId 考试ID
     */
    void submit(Long examId);
}
