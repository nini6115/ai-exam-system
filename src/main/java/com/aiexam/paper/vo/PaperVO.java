package com.aiexam.paper.vo;

import com.aiexam.paper.entity.ExamPaper;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷列表VO
 */
@Data
public class PaperVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 试卷ID */
    private Long id;

    /** 试卷名称 */
    private String name;

    /** 试卷描述 */
    private String description;

    /** 总分 */
    private BigDecimal totalScore;

    /** 及格分 */
    private BigDecimal passScore;

    /** 考试时长（分钟） */
    private Integer duration;

    /** 题目数量 */
    private Integer questionCount;

    /** 创建人ID */
    private Long creatorId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public static PaperVO from(ExamPaper p) {
        PaperVO vo = new PaperVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setDescription(p.getDescription());
        vo.setTotalScore(p.getTotalScore());
        vo.setPassScore(p.getPassScore());
        vo.setDuration(p.getDuration());
        vo.setQuestionCount(p.getQuestionCount());
        vo.setCreatorId(p.getCreatorId());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }
}
