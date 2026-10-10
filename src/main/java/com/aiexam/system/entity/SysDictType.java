package com.aiexam.system.entity;

import com.aiexam.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典类型实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 字典类型ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典名称（题型） */
    private String dictName;

    /** 字典编码（唯一业务键，关联字典数据，如 question_type） */
    private String dictCode;

    /** 描述 */
    private String description;

    /** 状态：1启用 0停用 */
    private Integer status;
}
