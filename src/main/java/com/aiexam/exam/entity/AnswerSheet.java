package com.aiexam.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 答卷实体（学生进入考试时创建，一张答卷对应一次考试）
 * <p>
 * 注意：answer_sheet 表没有 deleted 字段，故不继承 BaseEntity。
 */
@Data
@TableName("answer_sheet")
public class AnswerSheet implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 答卷ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试ID */
    private Long examId;

    /** 试卷ID（冗余） */
    private Long paperId;

    /** 考生ID */
    private Long userId;

    /** 第几次考试 */
    private Integer attemptNo;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 交卷时间 */
    private LocalDateTime submitTime;

    /** 应交卷时间（开始时间+时长，且不超过考试截止时间） */
    private LocalDateTime endTime;

    /** 状态：1答题中 2已交卷 3强制交卷 4超时自动交卷 */
    private Integer status;

    /** 总分 */
    private BigDecimal totalScore;

    /** 客观题得分 */
    private BigDecimal objectiveScore;

    /** 主观题得分 */
    private BigDecimal subjectiveScore;

    /** 是否通过：1是 0否 */
    private Integer isPassed;

    /** 切屏次数 */
    private Integer screenSwitchCount;

    /** 进入考试时的IP */
    private String ipAddress;

    /** 进入考试时的User-Agent */
    private String userAgent;

    /** 题目乱序种子 */
    private Integer questionOrderSeed;

    /** 交卷方式：1手动 2超时 3切屏超限 4管理员强制 */
    private Integer submitType;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
