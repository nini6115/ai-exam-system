package com.aiexam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增用户参数
 */
@Data
public class UserAddDTO {

    /** 登录账号 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度为2-50个字符")
    private String username;

    /** 初始密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
    private String password;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /** 学号/工号 */
    private String userNo;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 角色ID列表 */
    private List<Long> roleIds;
}
