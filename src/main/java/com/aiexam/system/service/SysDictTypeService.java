package com.aiexam.system.service;

import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.DictTypeAddDTO;
import com.aiexam.system.dto.DictTypeQueryDTO;
import com.aiexam.system.dto.DictTypeUpdateDTO;
import com.aiexam.system.entity.SysDictType;
import com.aiexam.system.vo.DictTypeVO;

import java.util.List;

/**
 * 数据字典类型服务（管理端）
 */
public interface SysDictTypeService {

    /**
     * 分页查询字典类型
     */
    PageVO<DictTypeVO> pageTypes(DictTypeQueryDTO dto);

    /**
     * 全量字典类型（前端类型下拉用）
     */
    List<DictTypeVO> listAll();

    /**
     * 字典类型详情
     */
    DictTypeVO getDetail(Long id);

    /**
     * 新增字典类型（dict_code 唯一）
     *
     * @return 新类型ID
     */
    Long addType(DictTypeAddDTO dto);

    /**
     * 修改字典类型（dict_code 不可修改）
     */
    void updateType(DictTypeUpdateDTO dto);

    /**
     * 删除字典类型（同事务级联物理删除其下字典数据，并清缓存）
     */
    void deleteType(Long id);
}
