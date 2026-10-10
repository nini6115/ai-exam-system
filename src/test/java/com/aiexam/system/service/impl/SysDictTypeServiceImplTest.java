package com.aiexam.system.service.impl;

import com.aiexam.common.context.LoginUser;
import com.aiexam.common.context.UserContext;
import com.aiexam.system.entity.SysDictData;
import com.aiexam.system.entity.SysDictType;
import com.aiexam.system.mapper.SysDictDataMapper;
import com.aiexam.system.mapper.SysDictTypeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据字典类型服务单元测试（Mockito，不依赖 MySQL/Redis）
 * <p>
 * 覆盖：code 唯一、code 不可改、删除级联与缓存清理（角色权限校验已迁移至 Controller）。
 */
@ExtendWith(MockitoExtension.class)
class SysDictTypeServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long TYPE_ID = 1L;
    private static final String DICT_CODE = "question_type";

    @Mock
    private SysDictTypeMapper sysDictTypeMapper;

    @Mock
    private SysDictDataMapper sysDictDataMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private SysDictTypeServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        // 类型（lambdaQuery/getById）与数据（删除级联的 LambdaQueryWrapper）都要解析列
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysDictType.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysDictData.class);
    }

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(ADMIN_ID, "admin", "系统管理员"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("新增：dict_code 重复应报错")
    void addType_duplicateCode_throws() {
        when(sysDictTypeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        var dto = new com.aiexam.system.dto.DictTypeAddDTO();
        dto.setDictName("题型");
        dto.setDictCode(DICT_CODE);

        assertThatThrownBy(() -> service.addType(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("字典编码已存在");
    }

    @Test
    @DisplayName("新增：成功返回新类型ID")
    void addType_success() {
        when(sysDictTypeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(sysDictTypeMapper.insert(any(SysDictType.class))).thenAnswer(invocation -> {
            SysDictType type = invocation.getArgument(0);
            type.setId(TYPE_ID);
            return 1;
        });
        var dto = new com.aiexam.system.dto.DictTypeAddDTO();
        dto.setDictName("题型");
        dto.setDictCode(DICT_CODE);

        Long id = service.addType(dto);

        assertThat(id).isEqualTo(TYPE_ID);
    }

    @Test
    @DisplayName("修改：dict_code 与库中不一致应报错（code 不可修改）")
    void updateType_codeChanged_throws() {
        when(sysDictTypeMapper.selectById(TYPE_ID)).thenReturn(buildType(DICT_CODE));
        var dto = new com.aiexam.system.dto.DictTypeUpdateDTO();
        dto.setId(TYPE_ID);
        dto.setDictName("题型");
        dto.setDictCode("other_code");

        assertThatThrownBy(() -> service.updateType(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("字典编码不可修改");
    }

    @Test
    @DisplayName("修改：编码一致时正常更新")
    void updateType_success() {
        when(sysDictTypeMapper.selectById(TYPE_ID)).thenReturn(buildType(DICT_CODE));
        var dto = new com.aiexam.system.dto.DictTypeUpdateDTO();
        dto.setId(TYPE_ID);
        dto.setDictName("题型（改）");
        dto.setDictCode(DICT_CODE);
        dto.setStatus(0);

        service.updateType(dto);

        ArgumentCaptor<SysDictType> captor = ArgumentCaptor.forClass(SysDictType.class);
        verify(sysDictTypeMapper).updateById(captor.capture());
        assertThat(captor.getValue().getDictName()).isEqualTo("题型（改）");
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
    }

    @Test
    @DisplayName("删除：逻辑删类型 + 级联物理删字典数据 + 清缓存")
    void deleteType_cascadesDataAndEvictsCache() {
        when(sysDictTypeMapper.selectById(TYPE_ID)).thenReturn(buildType(DICT_CODE));

        service.deleteType(TYPE_ID);

        verify(sysDictTypeMapper).deleteById(TYPE_ID);
        verify(sysDictDataMapper).delete(any(Wrapper.class));
        verify(redisTemplate).delete("dict:data:" + DICT_CODE);
    }

    @Test
    @DisplayName("删除：类型不存在应报错")
    void deleteType_missing_throws() {
        when(sysDictTypeMapper.selectById(TYPE_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteType(TYPE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("字典类型不存在");
    }

    private SysDictType buildType(String code) {
        SysDictType type = new SysDictType();
        type.setId(TYPE_ID);
        type.setDictName("题型");
        type.setDictCode(code);
        type.setStatus(1);
        return type;
    }
}
