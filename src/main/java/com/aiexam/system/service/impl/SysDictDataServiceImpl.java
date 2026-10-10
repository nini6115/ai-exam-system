package com.aiexam.system.service.impl;

import com.aiexam.common.context.UserContext;
import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.DictDataAddDTO;
import com.aiexam.system.dto.DictDataQueryDTO;
import com.aiexam.system.dto.DictDataUpdateDTO;
import com.aiexam.system.entity.SysDictData;
import com.aiexam.system.entity.SysDictType;
import com.aiexam.system.mapper.SysDictDataMapper;
import com.aiexam.system.mapper.SysDictTypeMapper;
import com.aiexam.system.service.SysDictDataService;
import com.aiexam.system.vo.DictDataVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 数据字典数据服务实现
 * <p>
 * 直接使用 Mapper（不继承 ServiceImpl），与全项目被测服务形态一致，便于纯单测。
 * listByTypeCode 是全系统高频读接口，走 Redis 缓存：命中直接返回；未命中查库后回写，
 * 非空 TTL 30 分钟、空列表 TTL 5 分钟防穿透；所有写操作后删除缓存
 * （Cache Aside：先更库后删缓存）。
 */
@Slf4j
@Service
public class SysDictDataServiceImpl implements SysDictDataService {

    /** 每页最大条数 */
    private static final int MAX_PAGE_SIZE = 100;
    /** 字典项查询缓存 key 前缀 */
    private static final String DATA_CACHE_KEY_PREFIX = "dict:data:";
    /** 缓存 TTL：非空 30 分钟 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    /** 缓存 TTL：空结果 5 分钟（防穿透） */
    private static final Duration CACHE_TTL_EMPTY = Duration.ofMinutes(5);
    /** 启用状态值 */
    private static final int STATUS_ENABLED = 1;
    /** ObjectMapper 仅处理 VO 列表，无日期字段无需注册模块 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private SysDictDataMapper sysDictDataMapper;

    @Autowired
    private SysDictTypeMapper sysDictTypeMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public PageVO<DictDataVO> pageDatas(DictDataQueryDTO dto) {
        Page<SysDictData> page = new Page<>(dto.getPageNum(),
                Math.min(dto.getPageSize(), MAX_PAGE_SIZE));
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictTypeCode, dto.getDictTypeCode())
                .like(dto.getLabel() != null && !dto.getLabel().isEmpty(),
                        SysDictData::getLabel, dto.getLabel())
                .eq(dto.getStatus() != null, SysDictData::getStatus, dto.getStatus())
                .orderByAsc(SysDictData::getSortOrder)
                .orderByAsc(SysDictData::getId);
        Page<SysDictData> result = sysDictDataMapper.selectPage(page, wrapper);
        List<DictDataVO> vos = result.getRecords().stream().map(DictDataVO::from).toList();
        return PageVO.of(page, vos);
    }

    @Override
    public List<DictDataVO> listByTypeCode(String dictTypeCode) {
        // 登录即可（前端下拉），不做角色校验
        String cacheKey = DATA_CACHE_KEY_PREFIX + dictTypeCode;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return OBJECT_MAPPER.readValue(cached, new TypeReference<List<DictDataVO>>() {});
            }
        } catch (Exception e) {
            // 缓存异常不阻断查询，降级直查数据库
            log.warn("读取字典缓存失败，降级查库，dictCode：{}", dictTypeCode, e);
        }

        List<DictDataVO> result;
        SysDictType type = sysDictTypeMapper.selectOne(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictCode, dictTypeCode));
        if (type == null || type.getStatus() == null || type.getStatus() != STATUS_ENABLED) {
            // 类型不存在或停用返回空列表
            result = List.of();
        } else {
            result = sysDictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                            .eq(SysDictData::getDictTypeCode, dictTypeCode)
                            .eq(SysDictData::getStatus, STATUS_ENABLED)
                            .orderByAsc(SysDictData::getSortOrder)
                            .orderByAsc(SysDictData::getId))
                    .stream().map(DictDataVO::from).toList();
        }

        // 回写缓存：空列表短 TTL 防穿透
        try {
            redisTemplate.opsForValue().set(cacheKey, OBJECT_MAPPER.writeValueAsString(result),
                    result.isEmpty() ? CACHE_TTL_EMPTY : CACHE_TTL);
        } catch (Exception e) {
            log.warn("写入字典缓存失败，dictCode：{}", dictTypeCode, e);
        }
        return result;
    }

    @Override
    public DictDataVO getDetail(Long id) {
        SysDictData data = sysDictDataMapper.selectById(id);
        if (data == null) {
            throw new RuntimeException("字典数据不存在");
        }
        return DictDataVO.from(data);
    }

    @Override
    public Long addData(DictDataAddDTO dto) {
        SysDictType type = requireType(dto.getDictTypeCode());
        if (type.getStatus() == null || type.getStatus() != STATUS_ENABLED) {
            throw new RuntimeException("字典类型已停用，不能新增字典项");
        }
        long count = sysDictDataMapper.selectCount(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictTypeCode, dto.getDictTypeCode())
                .eq(SysDictData::getValue, dto.getValue()));
        if (count > 0) {
            throw new RuntimeException("该字典下值已存在");
        }
        SysDictData data = new SysDictData();
        data.setDictTypeCode(dto.getDictTypeCode());
        data.setLabel(dto.getLabel());
        data.setValue(dto.getValue());
        data.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        data.setRemark(dto.getRemark());
        data.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
        sysDictDataMapper.insert(data);
        evictCache(dto.getDictTypeCode());
        log.info("用户[{}]新增字典项[{}({})]@{}",
                UserContext.getUsername(), dto.getLabel(), dto.getValue(), dto.getDictTypeCode());
        return data.getId();
    }

    @Override
    public void updateData(DictDataUpdateDTO dto) {
        SysDictData existing = sysDictDataMapper.selectById(dto.getId());
        if (existing == null) {
            throw new RuntimeException("字典数据不存在");
        }
        // 同类型下 value 唯一（排除自身）；不改属主类型
        long count = sysDictDataMapper.selectCount(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictTypeCode, existing.getDictTypeCode())
                .eq(SysDictData::getValue, dto.getValue())
                .ne(SysDictData::getId, dto.getId()));
        if (count > 0) {
            throw new RuntimeException("该字典下值已存在");
        }
        SysDictData update = new SysDictData();
        update.setId(dto.getId());
        update.setLabel(dto.getLabel());
        update.setValue(dto.getValue());
        update.setSortOrder(dto.getSortOrder() == null ? existing.getSortOrder() : dto.getSortOrder());
        update.setRemark(dto.getRemark() == null ? existing.getRemark() : dto.getRemark());
        update.setStatus(dto.getStatus() == null ? existing.getStatus() : dto.getStatus());
        sysDictDataMapper.updateById(update);
        evictCache(existing.getDictTypeCode());
        log.info("用户[{}]修改字典项[{}]@{}", UserContext.getUsername(), dto.getId(), existing.getDictTypeCode());
    }

    @Override
    public void deleteData(Long id) {
        SysDictData existing = sysDictDataMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("字典数据不存在");
        }
        sysDictDataMapper.deleteById(id);
        evictCache(existing.getDictTypeCode());
        log.info("用户[{}]删除字典项[{}({})]@{}",
                UserContext.getUsername(), existing.getLabel(), existing.getValue(), existing.getDictTypeCode());
    }

    /**
     * 要求字典类型存在
     */
    private SysDictType requireType(String dictTypeCode) {
        SysDictType type = sysDictTypeMapper.selectOne(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictCode, dictTypeCode));
        if (type == null) {
            throw new RuntimeException("字典类型不存在");
        }
        return type;
    }

    /**
     * 删除对应字典编码的查询缓存（Cache Aside）
     */
    private void evictCache(String dictTypeCode) {
        redisTemplate.delete(DATA_CACHE_KEY_PREFIX + dictTypeCode);
    }
}
