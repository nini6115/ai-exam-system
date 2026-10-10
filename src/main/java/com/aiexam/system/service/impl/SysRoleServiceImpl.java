package com.aiexam.system.service.impl;

import com.aiexam.common.constant.RoleConstants;
import com.aiexam.common.context.UserContext;
import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.RoleAddDTO;
import com.aiexam.system.dto.RoleUpdateDTO;
import com.aiexam.system.entity.SysRole;
import com.aiexam.system.entity.SysUser;
import com.aiexam.system.mapper.SysRoleMapper;
import com.aiexam.system.service.SysRoleService;
import com.aiexam.system.vo.RoleVO;
import com.aiexam.system.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色管理服务实现
 * <p>
 * 直接使用 Mapper（不继承 ServiceImpl，与全项目被测服务形态一致）。
 * sys_role 无 deleted 字段——删除为物理删除；删除用户时 sys_user_role 已同步清理，
 * 守卫计数直接数关联表即可。
 */
@Slf4j
@Service
public class SysRoleServiceImpl implements SysRoleService {

    /** 每页最大条数 */
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public List<RoleVO> listRoles() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .orderByAsc(SysRole::getId))
                .stream().map(RoleVO::from).toList();
    }

    @Override
    public RoleVO getRoleDetail(Long id) {
        return RoleVO.from(requireRole(id));
    }

    @Override
    public PageVO<UserVO> listRoleUsers(Long id, int pageNum, int pageSize) {
        requireRole(id);
        Page<SysUser> page = new Page<>(pageNum, Math.min(pageSize, MAX_PAGE_SIZE));
        IPage<SysUser> result = sysRoleMapper.selectUsersByRoleId(page, id);
        List<UserVO> vos = result.getRecords().stream().map(UserVO::from).toList();
        return PageVO.of(page, vos);
    }

    @Override
    public Long addRole(RoleAddDTO dto) {
        long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, dto.getRoleCode()));
        if (count > 0) {
            throw new RuntimeException("角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        sysRoleMapper.insert(role);
        log.info("用户[{}]新增角色[{}({})]", UserContext.getUsername(), dto.getRoleName(), dto.getRoleCode());
        return role.getId();
    }

    @Override
    public void updateRole(RoleUpdateDTO dto) {
        requireRole(dto.getId());
        SysRole update = new SysRole();
        update.setId(dto.getId());
        update.setRoleName(dto.getRoleName());
        update.setDescription(dto.getDescription());
        sysRoleMapper.updateById(update);
        log.info("用户[{}]修改角色[{}]", UserContext.getUsername(), dto.getId());
    }

    @Override
    public void deleteRole(Long id) {
        SysRole role = requireRole(id);
        if (RoleConstants.isBuiltin(role.getRoleCode())) {
            throw new RuntimeException("内置角色不允许删除");
        }
        long userCount = sysRoleMapper.countUsersByRoleId(id);
        if (userCount > 0) {
            throw new RuntimeException("该角色下已分配用户，请先移除后再删除");
        }
        sysRoleMapper.deleteById(id);
        log.info("用户[{}]删除角色[{}({})]", UserContext.getUsername(), role.getRoleName(), role.getRoleCode());
    }

    /**
     * 要求角色存在
     */
    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        return role;
    }
}
