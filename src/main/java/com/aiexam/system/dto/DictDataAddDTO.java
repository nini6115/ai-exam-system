package com.aiexam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 字典数据新增参数
 */
@Data
public class DictDataAddDTO {

    /** 所属字典编码 */
    @NotBlank(message = "所属字典编码不能为空")
    private String dictTypeCode;

    /** 显示名 */
    @NotBlank(message = "显示名不能为空")
    private String label;

    /** 存储值 */
    @NotBlank(message = "存储值不能为空")
    @Pattern(regexp = "^[^\\s]{1,100}$", message = "存储值不能含空白且不超过100字符")
    private String value;

    /** 排序号，默认 0 */
    private Integer sortOrder = 0;

    /** 备注 */
    private String remark;

    /** 状态：1启用 0停用 */
    private Integer status = 1;
}
