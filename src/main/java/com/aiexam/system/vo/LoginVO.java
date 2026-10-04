package com.aiexam.system.vo;

import com.aiexam.system.entity.SysRole;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录返回结果
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Token */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 头像 */
    private String avatar;

    /** 角色列表 */
    private List<SysRole> roles;
}
