package com.aiexam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人密码参数（当前登录用户，userId 从 Token 取）
 */
@Data
public class ChangePasswordDTO {

    /** 原密码 */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "新密码长度为6-100个字符")
    private String newPassword;
}
