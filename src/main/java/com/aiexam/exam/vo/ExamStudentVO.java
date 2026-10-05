package com.aiexam.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试考生VO（详情页学生列表用）
 */
@Data
public class ExamStudentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考生ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 学号 */
    private String userNo;

    /** 已考次数 */
    private Integer attempts;

    /** 最高分 */
    private BigDecimal bestScore;

    /** 分配时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assignTime;
}
