package com.aiexam.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 防作弊事件上报参数
 */
@Data
public class CheatReportDTO {

    /** 上报类型：1切屏 2离开超时（其余值 service 拒绝） */
    @NotNull(message = "上报类型不能为空")
    private Integer type;

    /** 补充描述（可选，service 截断到 500） */
    private String description;

    /** 详细信息 JSON 字符串（可选，如切屏前后页面信息，截断存 JSON 列） */
    private String detail;
}
