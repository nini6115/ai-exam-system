package com.aiexam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色修改参数
 * <p>
 * 不含 roleCode 字段——角色编码是鉴权锚点，结构性保证不可修改。
 */
@Data
public class RoleUpdateDTO {

    /** 角色ID */
    @NotNull(message = "角色ID不能为空")
    private Long id;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称不超过50字符")
    private String roleName;

    /** 描述 */
    @Size(max = 255, message = "描述不超过255字符")
    private String description;
}
