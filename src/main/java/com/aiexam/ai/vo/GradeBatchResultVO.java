package com.aiexam.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量 AI 判卷结果VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeBatchResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 触发时待判题数 */
    private int pendingCount;

    /** 判分成功数 */
    private int successCount;

    /** 判分失败数（失败题保持待判，可再次触发重试） */
    private int failedCount;

    /** 失败明细 */
    private List<GradeFailureVO> failures = new ArrayList<>();
}
