package com.aiexam.exam.dto;

import lombok.Data;

/**
 * 学生考试大厅查询参数
 */
@Data
public class ExamHallQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（service 中钳制） */
    private Integer pageSize = 10;

    /** 状态筛选：1待开始 2进行中 3已结束，null=全部 */
    private Integer status;
}
