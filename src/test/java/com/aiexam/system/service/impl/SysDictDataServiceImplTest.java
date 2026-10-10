package com.aiexam.system.service.impl;

import com.aiexam.common.context.LoginUser;
import com.aiexam.common.context.UserContext;
import com.aiexam.system.entity.SysDictData;
import com.aiexam.system.entity.SysDictType;
import com.aiexam.system.mapper.SysDictDataMapper;
import com.aiexam.system.mapper.SysDictTypeMapper;
import com.aiexam.system.vo.DictDataVO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据字典数据服务单元测试（Mockito，不依赖 MySQL/Redis）
 * <p>
 * 覆盖：类型校验、value 唯一、缓存命中/未命中/停用类型、写后清缓存。
 */
@ExtendWith(MockitoExtension.class)
class SysDictDataServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long DATA_ID = 11L;
    private static final String DICT_CODE = "question_type";
    private static final String CACHE_KEY = "dict:data:" + DICT_CODE;

    @Mock
    private SysDictDataMapper sysDictDataMapper;

    @Mock
    private SysDictTypeMapper sysDictTypeMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private SysDictDataServiceImpl service;

    /** setUp 中创建并被 redisTemplate.opsForValue() 返回的桩 */
    private ValueOperations<String, String> valueOps;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysDictData.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysDictType.class);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        UserContext.set(new LoginUser(ADMIN_ID, "admin", "系统管理员"));
        valueOps = mock(ValueOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ==================== 查询（缓存） ====================

    @Test
    @DisplayName("按类型查询：缓存命中时直接返回，完全不查库")
    void listByTypeCode_cacheHit() throws Exception {
        when(valueOps.get(CACHE_KEY)).thenReturn(cacheJson());

        List<DictDataVO> result = service.listByTypeCode(DICT_CODE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLabel()).isEqualTo("单选题");
        verify(sysDictTypeMapper, never()).selectOne(any(Wrapper.class));
        verify(sysDictDataMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("按类型查询：缓存未命中查库回写缓存（非空 30 分钟）")
    void listByTypeCode_cacheMiss_queriesDb() {
        stubEnabledType();
        when(sysDictDataMapper.selectList(any(Wrapper.class))).thenReturn(List.of(buildData()));

        List<DictDataVO> result = service.listByTypeCode(DICT_CODE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getValue()).isEqualTo("1");
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(setOperation()).set(eq(CACHE_KEY), jsonCaptor.capture(), ttlCaptor.capture());
        assertThat(jsonCaptor.getValue()).contains("单选题");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("按类型查询：类型不存在返回空列表并短 TTL 写缓存防穿透")
    void listByTypeCode_missingType_emptyWithShortTtl() {
        when(sysDictTypeMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        List<DictDataVO> result = service.listByTypeCode(DICT_CODE);

        assertThat(result).isEmpty();
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(setOperation()).set(eq(CACHE_KEY), anyString(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("按类型查询：类型停用返回空列表")
    void listByTypeCode_typeDisabled_empty() {
        SysDictType type = buildType();
        type.setStatus(0);
        when(sysDictTypeMapper.selectOne(any(Wrapper.class))).thenReturn(type);

        List<DictDataVO> result = service.listByTypeCode(DICT_CODE);

        assertThat(result).isEmpty();
        verify(sysDictDataMapper, never()).selectList(any(Wrapper.class));
    }

    // ==================== 新增 ====================

    @Test
    @DisplayName("新增：类型不存在应报错")
    void addData_typeMissing_throws() {
        when(sysDictTypeMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.addData(buildAddDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("字典类型不存在");
    }

    @Test
    @DisplayName("新增：类型停用应报错")
    void addData_typeDisabled_throws() {
        SysDictType type = buildType();
        type.setStatus(0);
        when(sysDictTypeMapper.selectOne(any(Wrapper.class))).thenReturn(type);

        assertThatThrownBy(() -> service.addData(buildAddDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("字典类型已停用，不能新增字典项");
    }

    @Test
    @DisplayName("新增：同类型下 value 重复应报错")
    void addData_duplicateValue_throws() {
        when(sysDictTypeMapper.selectOne(any(Wrapper.class))).thenReturn(buildType());
        when(sysDictDataMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.addData(buildAddDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("该字典下值已存在");
    }

    @Test
    @DisplayName("新增：成功落库并清缓存")
    void addData_success() {
        when(sysDictTypeMapper.selectOne(any(Wrapper.class))).thenReturn(buildType());
        when(sysDictDataMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            SysDictData data = invocation.getArgument(0);
            data.setId(DATA_ID);
            return 1;
        }).when(sysDictDataMapper).insert(any(SysDictData.class));

        Long id = service.addData(buildAddDTO());

        assertThat(id).isEqualTo(DATA_ID);
        verify(redisTemplate).delete(CACHE_KEY);
    }

    // ==================== 修改 / 删除 ====================

    @Test
    @DisplayName("修改：value 与同类型其他项冲突应报错")
    void updateData_duplicateValue_throws() {
        when(sysDictDataMapper.selectById(DATA_ID)).thenReturn(buildData());
        when(sysDictDataMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.updateData(buildUpdateDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("该字典下值已存在");
    }

    @Test
    @DisplayName("修改：成功更新并清缓存")
    void updateData_success() {
        when(sysDictDataMapper.selectById(DATA_ID)).thenReturn(buildData());
        when(sysDictDataMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service.updateData(buildUpdateDTO());

        verify(sysDictDataMapper).updateById(any(SysDictData.class));
        verify(redisTemplate).delete(CACHE_KEY);
    }

    @Test
    @DisplayName("删除：物理删除并清缓存")
    void deleteData_success() {
        when(sysDictDataMapper.selectById(DATA_ID)).thenReturn(buildData());

        service.deleteData(DATA_ID);

        verify(sysDictDataMapper).deleteById(DATA_ID);
        verify(redisTemplate).delete(CACHE_KEY);
    }

    // ==================== 测试数据 ====================

    private SysDictType buildType() {
        SysDictType type = new SysDictType();
        type.setId(1L);
        type.setDictName("题型");
        type.setDictCode(DICT_CODE);
        type.setStatus(1);
        return type;
    }

    private SysDictData buildData() {
        SysDictData data = new SysDictData();
        data.setId(DATA_ID);
        data.setDictTypeCode(DICT_CODE);
        data.setLabel("单选题");
        data.setValue("1");
        data.setSortOrder(1);
        data.setStatus(1);
        return data;
    }

    private com.aiexam.system.dto.DictDataAddDTO buildAddDTO() {
        com.aiexam.system.dto.DictDataAddDTO dto = new com.aiexam.system.dto.DictDataAddDTO();
        dto.setDictTypeCode(DICT_CODE);
        dto.setLabel("简答题");
        dto.setValue("5");
        return dto;
    }

    private com.aiexam.system.dto.DictDataUpdateDTO buildUpdateDTO() {
        com.aiexam.system.dto.DictDataUpdateDTO dto = new com.aiexam.system.dto.DictDataUpdateDTO();
        dto.setId(DATA_ID);
        dto.setLabel("单选题（改）");
        dto.setValue("1");
        return dto;
    }

    private void stubEnabledType() {
        when(sysDictTypeMapper.selectOne(any(Wrapper.class))).thenReturn(buildType());
    }

    private ValueOperations<String, String> setOperation() {
        return valueOps;
    }

    private String cacheJson() throws Exception {
        DictDataVO vo = new DictDataVO();
        vo.setId(DATA_ID);
        vo.setDictTypeCode(DICT_CODE);
        vo.setLabel("单选题");
        vo.setValue("1");
        return new ObjectMapper().writeValueAsString(List.of(vo));
    }
}
