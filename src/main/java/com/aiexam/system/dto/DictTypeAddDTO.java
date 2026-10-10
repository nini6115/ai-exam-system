package com.aiexam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 字典类型新增参数
 */
@Data
public class DictTypeAddDTO {

    /** 字典名称 */
    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    /** 字典编码（唯一业务键，小写字母/数字/下划线） */
    @NotBlank(message = "字典编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,49}$", message = "字典编码须为小写字母开头的字母/数字/下划线，2~50位")
    private String dictCode;

    /** 描述 */
    private String description;

    /** 状态：1启用 0停用 */
    private Integer status = 1;
}
