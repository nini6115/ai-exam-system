package com.aiexam.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试发布请求参数
 * <p>
 * 防作弊参数均可不传，Service 层按表默认值补齐。
 */
@Data
public class ExamPublishDTO {

    /** 考试名称 */
    @NotBlank(message = "考试名称不能为空")
    private String name;

    /** 试卷ID */
    @NotNull(message = "试卷ID不能为空")
    private Long paperId;

    /** 开始时间 */
    @NotNull(message = "考试开始时间不能为空")
    private LocalDateTime startTime;

    /** 截止时间 */
    @NotNull(message = "考试结束时间不能为空")
    private LocalDateTime endTime;

    /** 考试时长（分钟），不传则取试卷时长 */
    private Integer duration;

    /** 允许迟到分钟数，默认 0 */
    private Integer allowLateMinutes;

    /** 允许考试次数，默认 1 */
    private Integer maxAttempts;

    /** 题目乱序：1是 0否，默认 1 */
    private Integer randomOrder;

    /** 选项乱序：1是 0否，默认 1 */
    private Integer randomOptions;

    /** 最大切屏次数（0不限），默认 5 */
    private Integer maxScreenSwitch;

    /** 切屏超限处理：1警告 2强制交卷，默认 1 */
    private Integer screenSwitchAction;

    /** 离开超时秒数（0不限），默认 60 */
    private Integer awayTimeout;

    /** 禁止复制：1是 0否，默认 1 */
    private Integer forbidCopy;

    /** 交卷即显成绩：1是 0否，默认 0 */
    private Integer showScoreAfter;

    /** 参加考试的学生ID列表 */
    @NotEmpty(message = "请指定参加考试的学生")
    private List<Long> userIds;
}
