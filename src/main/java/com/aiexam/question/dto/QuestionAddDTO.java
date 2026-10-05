package com.aiexam.question.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增题目参数
 */
@Data
public class QuestionAddDTO {

    /** 题型：1单选 2多选 3判断 4填空 5简答（范围在 service 校验） */
    @NotNull(message = "题型不能为空")
    private Integer type;

    /** 难度：1简单 2中等 3困难，默认 2 */
    private Integer difficulty = 2;

    /** 知识点/分类 */
    @NotBlank(message = "分类不能为空")
    private String category;

    /** 题干 */
    @NotBlank(message = "题干不能为空")
    private String title;

    /** 选项（选择题必填，填空/简答忽略） */
    private List<String> options;

    /** 参考答案：单选"A"，多选"ABC"，判断"对"/"错"，填空/简答存文本 */
    @NotBlank(message = "参考答案不能为空")
    private String answer;

    /** 答案解析 */
    private String analysis;

    /** 默认分值 */
    @NotNull(message = "分值不能为空")
    @DecimalMin(value = "0.01", message = "分值必须大于0")
    private BigDecimal score;
}
