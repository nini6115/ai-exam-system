package com.aiexam.paper.entity;

import com.aiexam.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 试卷实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_paper")
public class ExamPaper extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 试卷ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 试卷名称 */
    private String name;

    /** 试卷描述 */
    private String description;

    /** 总分 */
    private BigDecimal totalScore;

    /** 及格分 */
    private BigDecimal passScore;

    /** 考试时长（分钟） */
    private Integer duration;

    /** 题目数量 */
    private Integer questionCount;

    /** 创建人ID */
    private Long creatorId;
}
