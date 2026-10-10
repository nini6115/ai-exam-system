package com.aiexam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 字典类型修改参数
 */
@Data
public class DictTypeUpdateDTO {

    /** 字典类型ID */
    @NotNull(message = "字典类型ID不能为空")
    private Long id;

    /** 字典名称 */
    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    /** 字典编码（只读，须与库中一致，service 校验） */
    @NotBlank(message = "字典编码不能为空")
    private String dictCode;

    /** 描述 */
    private String description;

    /** 状态：1启用 0停用 */
    private Integer status;
}
