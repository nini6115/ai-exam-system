package com.aiexam.paper.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 试卷中的单道题目VO（不含答案，教师/考生侧通用）
 */
@Data
public class PaperQuestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 题目ID */
    private Long questionId;

    /** 全卷排序（从1开始） */
    private Integer sortOrder;

    /** 大题分组名，如"单选题" */
    private String section;

    /** 本题分值 */
    private BigDecimal questionScore;

    /** 题型：1单选 2多选 3判断 4填空 5简答 */
    private Integer type;

    /** 题干 */
    private String title;

    /** 选项（选择题用，XML 中用 JacksonTypeHandler 映射 JSON） */
    private List<String> options;
}
