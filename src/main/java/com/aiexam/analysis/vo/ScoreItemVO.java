package com.aiexam.analysis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩明细行VO（exam_user 全名单 + 最新已交卷答卷）
 */
@Data
public class ScoreItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考生ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 学号 */
    private String userNo;

    /** 答卷状态：null未考 2已交卷 3强制交卷 4超时自动交卷 */
    private Integer sheetStatus;

    /** 交卷时间（未考为 null） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    /** 客观题得分（未考为 null） */
    private BigDecimal objectiveScore;

    /** 总分（当前=客观分，主观分待 AI 判卷；未考为 null） */
    private BigDecimal totalScore;

    /** 是否及格：1是 0否（未考为 null） */
    private Integer isPassed;

    /** 切屏次数（未考为 null） */
    private Integer screenSwitchCount;
}
