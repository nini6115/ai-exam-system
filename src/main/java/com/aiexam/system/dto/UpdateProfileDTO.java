package com.aiexam.system.dto;

import lombok.Data;

/**
 * 修改个人资料参数（当前登录用户，userId 从 Token 取）
 */
@Data
public class UpdateProfileDTO {

    /** 真实姓名 */
    private String realName;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;
}
