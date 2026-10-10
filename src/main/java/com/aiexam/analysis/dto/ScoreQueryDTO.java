package com.aiexam.analysis.dto;

import lombok.Data;

/**
 * 成绩明细分页查询参数
 */
@Data
public class ScoreQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（service 中钳制） */
    private Integer pageSize = 10;

    /** 姓名/学号模糊搜索，空=全部 */
    private String keyword;
}
