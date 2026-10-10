package com.aiexam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 字典数据修改参数
 * <p>
 * 不含 dictTypeCode：不允许更换属主字典类型。
 */
@Data
public class DictDataUpdateDTO {

    /** 字典数据ID */
    @NotNull(message = "字典数据ID不能为空")
    private Long id;

    /** 显示名 */
    @NotBlank(message = "显示名不能为空")
    private String label;

    /** 存储值 */
    @NotBlank(message = "存储值不能为空")
    private String value;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;

    /** 状态：1启用 0停用 */
    private Integer status;
}
