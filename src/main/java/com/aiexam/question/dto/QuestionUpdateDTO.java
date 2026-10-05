package com.aiexam.question.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改题目参数（其余字段同新增）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionUpdateDTO extends QuestionAddDTO {

    /** 题目ID */
    @NotNull(message = "题目ID不能为空")
    private Long id;
}
