package com.aiexam.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 定时保存答案 DTO（草稿态，只写 Redis）
 */
@Data
public class AnswerSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 题目ID */
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    /** 学生答案（空串表示清空该题） */
    private String answer;
}
