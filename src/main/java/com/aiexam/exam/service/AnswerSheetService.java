package com.aiexam.exam.service;

import com.aiexam.exam.dto.AnswerSaveDTO;
import com.aiexam.exam.dto.CheatReportDTO;
import com.aiexam.exam.vo.CheatReportVO;
import com.aiexam.exam.vo.ExamStartVO;

/**
 * 学生答题服务（进入考试 / 草稿保存 / 交卷 / 防作弊上报）
 */
public interface AnswerSheetService {

    /**
     * 学生进入考试：校验名单与时间次数、创建（或复用）答卷、返回乱序题目
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
     * <p>
     * 超过应交卷时间仍接受交卷（保住学生答案），但标记为超时自动交卷。
     *
     * @param examId 考试ID
     */
    void submit(Long examId);

    /**
     * 防作弊事件上报：切屏/离开超时写 cheat_record；切屏超限按配置警告或强制交卷
     *
     * @param examId 考试ID
     * @param dto    上报类型与描述
     * @return 当前切屏次数 + 是否超限 + 服务端动作
     */
    CheatReportVO reportCheat(Long examId, CheatReportDTO dto);

    /**
     * 扫描超时未交的答卷并逐张自动收卷（Quartz 定时调用，无登录态）
     *
     * @return 本轮成功收卷的张数
     */
    int autoSubmitTimeoutSheets();

    /**
     * 单张答卷超时自动收卷（独立事务，供批量循环逐张调用）
     *
     * @param sheetId 答卷ID
     * @return true=本次完成收卷；false=答卷不存在或已被其他入口收卷
     */
    boolean autoSubmit(Long sheetId);
}
