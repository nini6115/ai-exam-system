package com.aiexam.paper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 试卷-题目关联实体（纯关联表，无逻辑删除/时间字段，不继承 BaseEntity）
 */
@Data
@TableName("exam_paper_question")
public class ExamPaperQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 试卷ID */
    private Long paperId;

    /** 题目ID */
    private Long questionId;

    /** 该题分值 */
    private BigDecimal questionScore;

    /** 全卷排序（从1开始） */
    private Integer sortOrder;

    /** 大题分组名，如"单选题" */
    private String section;
}
