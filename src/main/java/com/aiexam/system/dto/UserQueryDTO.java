package com.aiexam.system.dto;

import lombok.Data;

/**
 * 用户分页查询参数
 */
@Data
public class UserQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（service 中钳制） */
    private Integer pageSize = 10;

    /** 用户名，模糊查询 */
    private String username;

    /** 真实姓名，模糊查询 */
    private String realName;

    /** 状态：0禁用 1正常 */
    private Integer status;

    /** 角色编码 */
    private String roleCode;
}
