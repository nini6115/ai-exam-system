package com.aiexam.system.service;

import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.DictDataAddDTO;
import com.aiexam.system.dto.DictDataQueryDTO;
import com.aiexam.system.dto.DictDataUpdateDTO;
import com.aiexam.system.vo.DictDataVO;

import java.util.List;

/**
 * 数据字典数据服务（管理端 CRUD + 登录即可的查询接口）
 */
public interface SysDictDataService {

    /**
     * 分页查询字典数据（按字典编码）
     */
    PageVO<DictDataVO> pageDatas(DictDataQueryDTO dto);

    /**
     * 按字典编码取启用字典项（登录即可；Redis 缓存，Cache Aside）
     *
     * @param dictTypeCode 字典编码
     * @return 启用项列表（类型不存在或停用返回空列表）
     */
    List<DictDataVO> listByTypeCode(String dictTypeCode);

    /**
     * 字典数据详情
     */
    DictDataVO getDetail(Long id);

    /**
     * 新增字典项（类型须存在且启用，同类型下 value 唯一）
     *
     * @return 新字典项ID
     */
    Long addData(DictDataAddDTO dto);

    /**
     * 修改字典项（不允许更换属主类型；value 同类型唯一）
     */
    void updateData(DictDataUpdateDTO dto);

    /**
     * 删除字典项（物理删除，并清缓存）
     */
    void deleteData(Long id);
}
