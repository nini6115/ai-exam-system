package com.aiexam.paper.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷详情VO（基本信息 + 题目列表，不含答案）
 */
@Data
public class PaperDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 试卷ID */
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

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 题目列表（按 sortOrder 升序，不含答案） */
    private List<PaperQuestionVO> questions;
}
