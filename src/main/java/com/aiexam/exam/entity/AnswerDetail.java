package com.aiexam.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 答题明细实体（一张答卷的逐题作答与判分记录）
 * <p>
 * 注意：answer_detail 表没有 deleted / create_time / update_time 字段，不继承 BaseEntity。
 */
@Data
@TableName("answer_detail")
public class AnswerDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 答卷ID */
    private Long sheetId;

    /** 题目ID */
    private Long questionId;

    /** 考生答案 */
    private String userAnswer;

    /** 参考答案（冗余） */
    private String correctAnswer;

    /** 本题得分 */
    private BigDecimal score;

    /** 本题满分（冗余） */
    private BigDecimal fullScore;

    /** 是否正确：1是 0否 NULL(主观题待判) */
    private Integer isCorrect;

    /** 答卷中的题号 */
    private Integer sortOrder;

    /** 最后作答时间 */
    private LocalDateTime answerTime;

    /** 判分方式：1系统 2人工 3 AI */
    private Integer gradedBy;

    /** 判分人ID */
    private Long graderId;

    /** 判分时间 */
    private LocalDateTime gradeTime;

    /** 判分评语 */
    private String gradeComment;
}
