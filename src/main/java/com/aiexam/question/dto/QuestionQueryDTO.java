package com.aiexam.question.dto;

import lombok.Data;

/**
 * 题目分页查询参数
 */
@Data
public class QuestionQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（service 中钳制） */
    private Integer pageSize = 10;

    /** 题型：1单选 2多选 3判断 4填空 5简答 */
    private Integer type;

    /** 难度：1简单 2中等 3困难 */
    private Integer difficulty;

    /** 知识点/分类，精确查询 */
    private String category;

    /** 题干，模糊查询 */
    private String title;
}
