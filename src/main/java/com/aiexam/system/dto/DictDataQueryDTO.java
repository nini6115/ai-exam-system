package com.aiexam.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典数据分页查询参数（按字典编码必填）
 */
@Data
public class DictDataQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（service 中钳制） */
    private Integer pageSize = 10;

    /** 所属字典编码（必填） */
    @NotBlank(message = "所属字典编码不能为空")
    private String dictTypeCode;

    /** 显示名，模糊查询 */
    private String label;

    /** 状态：1启用 0停用，null=全部 */
    private Integer status;
}
