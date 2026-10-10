package com.aiexam.system.vo;

import com.aiexam.system.entity.SysDictData;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据字典数据VO
 */
@Data
public class DictDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字典数据ID */
    private Long id;

    /** 所属字典编码 */
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

    public static DictDataVO from(SysDictData entity) {
        DictDataVO vo = new DictDataVO();
        vo.setId(entity.getId());
        vo.setDictTypeCode(entity.getDictTypeCode());
        vo.setLabel(entity.getLabel());
        vo.setValue(entity.getValue());
        vo.setSortOrder(entity.getSortOrder());
        vo.setRemark(entity.getRemark());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
