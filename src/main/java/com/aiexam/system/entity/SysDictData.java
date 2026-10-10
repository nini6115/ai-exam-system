package com.aiexam.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据字典数据实体
 * <p>
 * 注意：sys_dict_data 表物理删除，没有 deleted 字段（避免 @TableLogic 与
 * uk(dict_type_code, value) 唯一键冲突），故不继承 BaseEntity。
 */
@Data
@TableName("sys_dict_data")
public class SysDictData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字典数据ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属字典编码（关联 sys_dict_type.dict_code） */
    private String dictTypeCode;

    /** 显示名 */
    private String label;

    /** 存储值 */
    private String value;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
