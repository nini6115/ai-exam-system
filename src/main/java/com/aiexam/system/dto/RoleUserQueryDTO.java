package com.aiexam.system.dto;

import lombok.Data;

/**
 * 角色下用户分页查询参数
 */
@Data
public class RoleUserQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（service 中钳制） */
    private Integer pageSize = 10;
}
