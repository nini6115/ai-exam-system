package com.aiexam.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试-考生关联实体
 * <p>
 * 注意：exam_user 表没有 deleted / update_time 字段，不继承 BaseEntity。
 */
@Data
@TableName("exam_user")
public class ExamUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试ID */
    private Long examId;

    /** 考生ID */
    private Long userId;

    /** 已考次数 */
    private Integer attempts;

    /** 最高分 */
    private BigDecimal bestScore;

    /** 分配时间 */
    private LocalDateTime assignTime;
}
