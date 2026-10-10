package com.aiexam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色新增参数
 */
@Data
public class RoleAddDTO {

    /** 角色编码（唯一业务键，鉴权锚点） */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码不超过50字符")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{1,49}$", message = "角色编码须为字母开头的字母/数字/下划线，2~50位")
    private String roleCode;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称不超过50字符")
    private String roleName;

    /** 描述 */
    @Size(max = 255, message = "描述不超过255字符")
    private String description;
}
