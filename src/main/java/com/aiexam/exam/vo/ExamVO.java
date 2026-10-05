package com.aiexam.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考试列表VO
 * <p>
 * status 为按当前时间实时推导的状态：1未开始 2进行中 3已结束。
 */
@Data
public class ExamVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考试ID */
    private Long id;

    /** 考试名称 */
    private String name;

    /** 试卷ID */
    private Long paperId;

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

    /** 状态：1未开始 2进行中 3已结束（实时推导） */
    private Integer status;

    /** 允许考试次数 */
    private Integer maxAttempts;

    /** 题目乱序：1是 0否 */
    private Integer randomOrder;

    /** 最大切屏次数（0不限） */
    private Integer maxScreenSwitch;

    /** 参加考试人数 */
    private Integer studentCount;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
