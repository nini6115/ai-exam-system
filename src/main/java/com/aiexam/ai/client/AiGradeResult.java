package com.aiexam.ai.client;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 结构化判分结果（客户端契约对象，非前端 VO）
 */
@Data
@AllArgsConstructor
public class AiGradeResult {

    /** 分数（未经满分校验的原始值） */
    private BigDecimal score;

    /** 评语，可为 null */
    private String comment;
}
