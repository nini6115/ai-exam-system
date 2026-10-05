package com.aiexam.exam.vo;

import com.aiexam.paper.vo.PaperQuestionVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生进入考试的返回 VO（题目列表不含答案与解析）
 */
@Data
public class ExamStartVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考试ID */
    private Long examId;

    /** 考试名称 */
    private String examName;

    /** 答卷ID */
    private Long sheetId;

    /** 开始答题时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 应交卷时间（前端据此倒计时） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 考试时长（分钟） */
    private Integer duration;

    /** 题目乱序：1是 0否（返回的题目已按种子打乱） */
    private Integer randomOrder;

    /** 选项乱序：1是 0否（标记位，由前端处理） */
    private Integer randomOptions;

    /** 题目列表（不含答案、解析） */
    private List<PaperQuestionVO> questions;
}
