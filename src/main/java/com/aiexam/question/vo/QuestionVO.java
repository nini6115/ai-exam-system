package com.aiexam.question.vo;

import com.aiexam.question.entity.Question;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目列表/详情VO
 */
@Data
public class QuestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 题目ID */
    private Long id;

    /** 题型：1单选 2多选 3判断 4填空 5简答 */
    private Integer type;

    /** 难度：1简单 2中等 3困难 */
    private Integer difficulty;

    /** 知识点/分类 */
    private String category;

    /** 题干 */
    private String title;

    /** 选项（选择题用） */
    private List<String> options;

    /** 参考答案 */
    private String answer;

    /** 答案解析 */
    private String analysis;

    /** 默认分值 */
    private BigDecimal score;

    /** 创建人ID */
    private Long creatorId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public static QuestionVO from(Question q) {
        QuestionVO vo = new QuestionVO();
        vo.setId(q.getId());
        vo.setType(q.getType());
        vo.setDifficulty(q.getDifficulty());
        vo.setCategory(q.getCategory());
        vo.setTitle(q.getTitle());
        vo.setOptions(q.getOptions());
        vo.setAnswer(q.getAnswer());
        vo.setAnalysis(q.getAnalysis());
        vo.setScore(q.getScore());
        vo.setCreatorId(q.getCreatorId());
        vo.setCreateTime(q.getCreateTime());
        return vo;
    }
}
