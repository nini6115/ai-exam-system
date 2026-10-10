package com.aiexam.system.vo;

import com.aiexam.system.entity.SysDictType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据字典类型VO
 */
@Data
public class DictTypeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字典类型ID */
    private Long id;

    /** 字典名称 */
    private String dictName;

    /** 字典编码 */
    private String dictCode;

    /** 描述 */
    private String description;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public static DictTypeVO from(SysDictType entity) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(entity.getId());
        vo.setDictName(entity.getDictName());
        vo.setDictCode(entity.getDictCode());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
