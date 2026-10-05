package com.aiexam.exam.dto;

import lombok.Data;

/**
 * 考试分页查询参数
 */
@Data
public class ExamQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（service 中钳制） */
    private Integer pageSize = 10;

    /** 考试名称，模糊查询 */
    private String name;
}
