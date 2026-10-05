package com.aiexam.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考试发布实体
 * <p>
 * 注意：exam 表没有 deleted 字段，故不继承 BaseEntity，逻辑删除条件不会拼进 SQL。
 */
@Data
@TableName("exam")
public class Exam implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考试ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试名称 */
    private String name;

    /** 试卷ID */
    private Long paperId;

    /** 发布人ID */
    private Long creatorId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 截止时间 */
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

    /** 状态：1未开始 2进行中 3已结束（落库值，展示状态按时间实时推导） */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
