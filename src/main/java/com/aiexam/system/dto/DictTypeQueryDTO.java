package com.aiexam.system.dto;

import lombok.Data;

/**
 * 字典类型分页查询参数
 */
@Data
public class DictTypeQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（service 中钳制） */
    private Integer pageSize = 10;

    /** 字典名称，模糊查询 */
    private String dictName;

    /** 字典编码，模糊查询 */
    private String dictCode;

    /** 状态：1启用 0停用，null=全部 */
    private Integer status;
}
