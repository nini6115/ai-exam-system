package com.aiexam.paper.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 手动组卷参数
 */
@Data
public class PaperCreateDTO {

    /** 试卷标题 */
    @NotBlank(message = "试卷标题不能为空")
    private String title;

    /** 试卷描述 */
    private String description;

    /** 考试时长（分钟） */
    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长必须大于0")
    private Integer duration;

    /** 及格分，不传默认总分的60% */
    @DecimalMin(value = "0.01", message = "及格分必须大于0")
    private BigDecimal passScore;

    /** 题目ID列表（按题型分组排序后入卷） */
    @NotEmpty(message = "题目ID列表不能为空")
    private List<Long> questionIds;
}
