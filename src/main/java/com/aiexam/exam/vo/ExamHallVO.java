package com.aiexam.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生考试大厅列表VO
 * <p>
 * status 为按当前时间实时推导的状态：1待开始 2进行中 3已结束。
 */
@Data
public class ExamHallVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考试ID */
    private Long examId;

    /** 考试名称 */
    private String examName;

    /** 试卷名称 */
    private String paperName;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 考试时长（分钟） */
    private Integer duration;

    /** 状态：1待开始 2进行中 3已结束（实时推导） */
    private Integer status;

    /** 允许考试次数 */
    private Integer maxAttempts;

    /** 我的已考次数 */
    private Integer myAttempts;

    /** 最新答卷ID，未进入过为 null */
    private Long sheetId;

    /** 最新答卷状态：1答题中 2已交卷 3强制交卷 4超时自动交卷，未进入过为 null */
    private Integer sheetStatus;

    /** 交卷即显成绩：1是 0否 */
    private Integer showScoreAfter;

    /** 分数是否可见（服务端计算：已交卷且 showScoreAfter=1） */
    private Boolean scoreVisible;

    /** 最高分（scoreVisible=false 时为 null） */
    private BigDecimal bestScore;
}
