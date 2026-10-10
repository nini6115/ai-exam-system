package com.aiexam.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 判分失败明细VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeFailureVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 答卷ID */
    private Long sheetId;

    /** 题目ID */
    private Long questionId;

    /** 失败原因 */
    private String reason;
}
