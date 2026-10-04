package com.aiexam.system.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long id;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;
}
