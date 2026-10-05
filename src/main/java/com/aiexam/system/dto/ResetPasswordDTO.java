package com.aiexam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码参数
 */
@Data
public class ResetPasswordDTO {

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "新密码长度为6-100个字符")
    private String newPassword;
}
