package com.aiexam.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试详情VO（教师端）
 */
@Data
public class ExamDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考试ID */
    private Long id;

    /** 考试名称 */
    private String name;

    /** 试卷ID */
    private Long paperId;

    /** 试卷名称 */
    private String paperName;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 试卷及格分 */
    private BigDecimal passScore;

    /** 题目数量 */
    private Integer questionCount;

    /** 发布人ID */
    private Long creatorId;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 考试时长（分钟） */
    private Integer duration;

    /** 允许迟到分钟数 */
    private Integer allowLateMinutes;

    /** 允许考试次数 */
    private Integer maxAttempts;

    /** 题目乱序：1是 0否 */
    private Integer randomOrder;

    /** 选项乱序：1是 0否 */
    private Integer randomOptions;

    /** 最大切屏次数（0不限） */
    private Integer maxScreenSwitch;

    /** 切屏超限处理：1警告 2强制交卷 */
    private Integer screenSwitchAction;

    /** 离开超时秒数（0不限） */
    private Integer awayTimeout;

    /** 禁止复制：1是 0否 */
    private Integer forbidCopy;

    /** 交卷即显成绩：1是 0否 */
    private Integer showScoreAfter;

    /** 状态：1未开始 2进行中 3已结束（实时推导） */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 参加考试的学生列表 */
    private List<ExamStudentVO> students;
}
