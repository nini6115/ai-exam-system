package com.aiexam.analysis.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 分数段分布VO（按得分率分段，供前端渲染柱状图）
 */
@Data
public class ScoreSegmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 段位：1(≥90%) 2(80%~89%) 3(70%~79%) 4(60%~69%) 5(<60%) */
    private Integer level;

    /** 段位名称 */
    private String label;

    /** 该段人数 */
    private Long studentCount;

    public ScoreSegmentVO() {
    }

    public ScoreSegmentVO(Integer level, String label, Long studentCount) {
        this.level = level;
        this.label = label;
        this.studentCount = studentCount;
    }
}
