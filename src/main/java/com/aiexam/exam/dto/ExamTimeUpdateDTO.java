package com.aiexam.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调整考试时间请求参数（教师/管理员，如小测试延时/提前结束）
 */
@Data
public class ExamTimeUpdateDTO {

    /** 开始时间 */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /** 结束时间 */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
