package com.aiexam.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启用/禁用用户参数
 */
@Data
public class UpdateStatusDTO {

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 状态：0禁用 1正常 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
