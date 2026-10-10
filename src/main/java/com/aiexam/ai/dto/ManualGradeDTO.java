package com.aiexam.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 人工改分入参
 */
@Data
public class ManualGradeDTO {

    /** 本题得分（范围校验在 Service：需要明细上的满分） */
    @NotNull(message = "分数不能为空")
    private BigDecimal score;

    /** 改分评语，可选；不传则保留原评语（保留 AI 评语供复核留痕） */
    private String comment;
}
