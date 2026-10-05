package com.aiexam.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 修改用户参数（不能修改 username 和 password）
 */
@Data
public class UserUpdateDTO {

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /** 真实姓名 */
    private String realName;

    /** 学号/工号 */
    private String userNo;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;

    /** 状态：0禁用 1正常 */
    private Integer status;

    /** 角色ID列表（传了就全量覆盖，不传则不改动） */
    private List<Long> roleIds;
}
