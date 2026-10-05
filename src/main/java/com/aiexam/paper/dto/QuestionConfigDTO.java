package com.aiexam.paper.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单条抽题配置：按题型/难度（/分类）随机抽取 count 道题，每题 scorePerQuestion 分
 */
@Data
public class QuestionConfigDTO {

    /** 题型：1单选 2多选 3判断 4填空 5简答 */
    @NotNull(message = "题型不能为空")
    @Min(value = 1, message = "题型只能为1-5")
    @Max(value = 5, message = "题型只能为1-5")
    private Integer type;

    /** 难度：1简单 2中等 3困难 */
    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度只能为1-3")
    @Max(value = 3, message = "难度只能为1-3")
    private Integer difficulty;

    /** 抽题数量 */
    @NotNull(message = "抽题数量不能为空")
    @Min(value = 1, message = "抽题数量至少为1")
    private Integer count;

    /** 每题分值 */
    @NotNull(message = "每题分值不能为空")
    @DecimalMin(value = "0.01", message = "每题分值必须大于0")
    private BigDecimal scorePerQuestion;

    /** 分类（可选），不传则用试卷级 category，都为空则不限分类 */
    private String category;
}
