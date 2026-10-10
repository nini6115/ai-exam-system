package com.aiexam.system.service;

import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.RoleAddDTO;
import com.aiexam.system.dto.RoleUpdateDTO;
import com.aiexam.system.vo.RoleVO;
import com.aiexam.system.vo.UserVO;

import java.util.List;

/**
 * 角色管理服务（权限校验在 Controller @RequiresRoles 完成）
 */
public interface SysRoleService {

    /**
     * 全量角色列表（少量配置数据不分页）
     */
    List<RoleVO> listRoles();

    /**
     * 角色详情
     */
    RoleVO getRoleDetail(Long id);

    /**
     * 角色下的用户分页
     */
    PageVO<UserVO> listRoleUsers(Long id, int pageNum, int pageSize);

    /**
     * 新增角色（role_code 唯一）
     *
     * @return 新角色ID
     */
    Long addRole(RoleAddDTO dto);

    /**
     * 修改角色（role_code 不可修改，由 DTO 结构保证）
     */
    void updateRole(RoleUpdateDTO dto);

    /**
     * 删除角色（内置角色禁删；角色下有用户禁删）
     */
    void deleteRole(Long id);
}
