package com.aiexam.system.service.impl;

import com.aiexam.common.context.UserContext;
import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.DictTypeAddDTO;
import com.aiexam.system.dto.DictTypeQueryDTO;
import com.aiexam.system.dto.DictTypeUpdateDTO;
import com.aiexam.system.entity.SysDictData;
import com.aiexam.system.entity.SysDictType;
import com.aiexam.system.mapper.SysDictDataMapper;
import com.aiexam.system.mapper.SysDictTypeMapper;
import com.aiexam.system.service.SysDictTypeService;
import com.aiexam.system.vo.DictTypeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据字典类型服务实现
 * <p>
 * 直接使用 Mapper（不继承 ServiceImpl），与全项目被测服务形态一致，便于纯单测。
 * 删除类型时同事务级联物理删除其下字典数据（字典是展示元数据，业务表存的是裸值，
 * 级联删不影响业务数据），并清除对应查询缓存。
 */
@Slf4j
@Service
public class SysDictTypeServiceImpl implements SysDictTypeService {

    /** 每页最大条数 */
    private static final int MAX_PAGE_SIZE = 100;
    /** 字典项查询缓存 key 前缀（与 SysDictDataServiceImpl 保持一致） */
    private static final String DATA_CACHE_KEY_PREFIX = "dict:data:";
    /** 启用状态值 */
    private static final int STATUS_ENABLED = 1;

    @Autowired
    private SysDictTypeMapper sysDictTypeMapper;

    @Autowired
    private SysDictDataMapper sysDictDataMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public PageVO<DictTypeVO> pageTypes(DictTypeQueryDTO dto) {
        Page<SysDictType> page = new Page<>(dto.getPageNum(),
                Math.min(dto.getPageSize(), MAX_PAGE_SIZE));
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .like(dto.getDictName() != null && !dto.getDictName().isEmpty(),
                        SysDictType::getDictName, dto.getDictName())
                .like(dto.getDictCode() != null && !dto.getDictCode().isEmpty(),
                        SysDictType::getDictCode, dto.getDictCode())
                .eq(dto.getStatus() != null, SysDictType::getStatus, dto.getStatus())
                .orderByAsc(SysDictType::getId);
        Page<SysDictType> result = sysDictTypeMapper.selectPage(page, wrapper);
        List<DictTypeVO> vos = result.getRecords().stream().map(DictTypeVO::from).toList();
        return PageVO.of(page, vos);
    }

    @Override
    public List<DictTypeVO> listAll() {
        return sysDictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                        .orderByAsc(SysDictType::getId))
                .stream().map(DictTypeVO::from).toList();
    }

    @Override
    public DictTypeVO getDetail(Long id) {
        SysDictType type = sysDictTypeMapper.selectById(id);
        if (type == null) {
            throw new RuntimeException("字典类型不存在");
        }
        return DictTypeVO.from(type);
    }

    @Override
    public Long addType(DictTypeAddDTO dto) {
        long count = sysDictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictCode, dto.getDictCode()));
        if (count > 0) {
            throw new RuntimeException("字典编码已存在");
        }
        SysDictType type = new SysDictType();
        type.setDictName(dto.getDictName());
        type.setDictCode(dto.getDictCode());
        type.setDescription(dto.getDescription());
        type.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
        sysDictTypeMapper.insert(type);
        log.info("用户[{}]新增字典类型[{}({})]", UserContext.getUsername(), type.getDictName(), type.getDictCode());
        return type.getId();
    }

    @Override
    public void updateType(DictTypeUpdateDTO dto) {
        SysDictType existing = sysDictTypeMapper.selectById(dto.getId());
        if (existing == null) {
            throw new RuntimeException("字典类型不存在");
        }
        // code 是数据关联与缓存键，不允许修改
        if (!dto.getDictCode().equals(existing.getDictCode())) {
            throw new RuntimeException("字典编码不可修改");
        }
        SysDictType update = new SysDictType();
        update.setId(dto.getId());
        update.setDictName(dto.getDictName());
        update.setDescription(dto.getDescription());
        update.setStatus(dto.getStatus() == null ? existing.getStatus() : dto.getStatus());
        sysDictTypeMapper.updateById(update);
        log.info("用户[{}]修改字典类型[{}({})]", UserContext.getUsername(), dto.getDictName(), dto.getDictCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        SysDictType existing = sysDictTypeMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("字典类型不存在");
        }
        // 级联物理删除其下字典数据（uk(dict_type_code,value) 无逻辑删位，物理删干净）
        sysDictTypeMapper.deleteById(id);
        sysDictDataMapper.delete(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictTypeCode, existing.getDictCode()));
        redisTemplate.delete(DATA_CACHE_KEY_PREFIX + existing.getDictCode());
        log.info("用户[{}]删除字典类型[{}({})]，已级联删除字典数据",
                UserContext.getUsername(), existing.getDictName(), existing.getDictCode());
    }
}
