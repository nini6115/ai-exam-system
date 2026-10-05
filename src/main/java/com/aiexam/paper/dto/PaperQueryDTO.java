package com.aiexam.paper.dto;

import lombok.Data;

/**
 * 试卷分页查询参数
 */
@Data
public class PaperQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（service 中钳制） */
    private Integer pageSize = 10;

    /** 试卷名称，模糊查询 */
    private String name;
}
