package com.aiexam.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配角色参数
 */
@Data
public class AssignRoleDTO {

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 角色ID列表，可为空数组（清空角色） */
    @NotNull(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
