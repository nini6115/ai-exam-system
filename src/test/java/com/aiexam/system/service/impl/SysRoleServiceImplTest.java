package com.aiexam.system.service.impl;

import com.aiexam.common.context.LoginUser;
import com.aiexam.common.context.UserContext;
import com.aiexam.system.entity.SysRole;
import com.aiexam.system.entity.SysUser;
import com.aiexam.system.mapper.SysRoleMapper;
import com.aiexam.system.vo.RoleVO;
import com.aiexam.system.vo.UserVO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色管理服务单元测试（Mockito，不依赖 MySQL）
 * <p>
 * 覆盖：CRUD 规则（编码唯一/编码不可改/内置角色禁删/被引用禁删）。
 */
@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long ROLE_ID = 2L;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @InjectMocks
    private SysRoleServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        // roleCode 唯一校验的 LambdaQueryWrapper 解析列需要 MP 的 TableInfo 缓存
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysRole.class);
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
    @DisplayName("列表：实体转 VO 映射正确")
    void listRoles_mapsToVO() {
        when(sysRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(buildRole(ROLE_ID, "teacher")));

        List<RoleVO> list = service.listRoles();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getRoleCode()).isEqualTo("teacher");
        assertThat(list.get(0).getRoleName()).isEqualTo("教师");
    }

    @Test
    @DisplayName("详情：不存在应报错")
    void getRoleDetail_missing_throws() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.getRoleDetail(ROLE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("角色不存在");
    }

    @Test
    @DisplayName("新增：编码重复应报错")
    void addRole_duplicateCode_throws() {
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        var dto = buildAddDTO();

        assertThatThrownBy(() -> service.addRole(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("角色编码已存在");
    }

    @Test
    @DisplayName("新增：成功返回新角色ID")
    void addRole_success() {
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            SysRole role = invocation.getArgument(0);
            role.setId(ROLE_ID);
            return 1;
        }).when(sysRoleMapper).insert(any(SysRole.class));
        var dto = buildAddDTO();

        Long id = service.addRole(dto);

        assertThat(id).isEqualTo(ROLE_ID);
        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(sysRoleMapper).insert(captor.capture());
        assertThat(captor.getValue().getRoleCode()).isEqualTo("tutor");
        assertThat(captor.getValue().getRoleName()).isEqualTo("助教");
    }

    @Test
    @DisplayName("修改：角色不存在应报错")
    void updateRole_missing_throws() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(null);
        var dto = buildUpdateDTO();

        assertThatThrownBy(() -> service.updateRole(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("角色不存在");
    }

    @Test
    @DisplayName("修改：落库实体不含 roleCode（结构性不可改）")
    void updateRole_roleCodeNeverTouched() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(buildRole(ROLE_ID, "tutor"));

        service.updateRole(buildUpdateDTO());

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(sysRoleMapper).updateById(captor.capture());
        assertThat(captor.getValue().getRoleCode()).isNull();
        assertThat(captor.getValue().getRoleName()).isEqualTo("助教（改）");
    }

    @Test
    @DisplayName("删除：不存在应报错")
    void deleteRole_missing_throws() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteRole(ROLE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("角色不存在");
    }

    @Test
    @DisplayName("删除：内置角色（admin/teacher/student）禁删")
    void deleteRole_builtin_throws() {
        for (String builtinCode : new String[]{"admin", "teacher", "student"}) {
            when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(buildRole(ROLE_ID, builtinCode));

            assertThatThrownBy(() -> service.deleteRole(ROLE_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("内置角色不允许删除");
        }
    }

    @Test
    @DisplayName("删除：角色下有用户应报错")
    void deleteRole_hasUsers_throws() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(buildRole(ROLE_ID, "tutor"));
        when(sysRoleMapper.countUsersByRoleId(ROLE_ID)).thenReturn(3L);

        assertThatThrownBy(() -> service.deleteRole(ROLE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("该角色下已分配用户，请先移除后再删除");
    }

    @Test
    @DisplayName("删除：通过守卫后物理删除")
    void deleteRole_success() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(buildRole(ROLE_ID, "tutor"));
        when(sysRoleMapper.countUsersByRoleId(ROLE_ID)).thenReturn(0L);

        service.deleteRole(ROLE_ID);

        verify(sysRoleMapper).deleteById(ROLE_ID);
    }

    @Test
    @DisplayName("角色下用户分页：实体转 UserVO")
    void listRoleUsers_mapsToUserVO() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(buildRole(ROLE_ID, "teacher"));
        when(sysRoleMapper.selectUsersByRoleId(any(Page.class), any()))
                .thenAnswer(invocation -> {
                    Page<SysUser> page = invocation.getArgument(0);
                    page.setRecords(List.of(buildUser()));
                    page.setTotal(1);
                    return page;
                });

        var vo = service.listRoleUsers(ROLE_ID, 1, 10);

        assertThat(vo.getList()).hasSize(1);
        assertThat(vo.getList().get(0).getUsername()).isEqualTo("tom");
    }

    // ==================== 测试数据 ====================

    private SysRole buildRole(Long id, String roleCode) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName("tutor".equals(roleCode) ? "助教"
                : "admin".equals(roleCode) ? "超级管理员"
                : "teacher".equals(roleCode) ? "教师"
                : "student".equals(roleCode) ? "学生" : roleCode);
        return role;
    }

    private SysUser buildUser() {
        SysUser user = new SysUser();
        user.setId(100L);
        user.setUsername("tom");
        user.setRealName("汤姆");
        user.setStatus(1);
        return user;
    }

    private com.aiexam.system.dto.RoleAddDTO buildAddDTO() {
        var dto = new com.aiexam.system.dto.RoleAddDTO();
        dto.setRoleCode("tutor");
        dto.setRoleName("助教");
        dto.setDescription("批改助教");
        return dto;
    }

    private com.aiexam.system.dto.RoleUpdateDTO buildUpdateDTO() {
        var dto = new com.aiexam.system.dto.RoleUpdateDTO();
        dto.setId(ROLE_ID);
        dto.setRoleName("助教（改）");
        return dto;
    }
}
