package com.aiexam.system.controller;

import com.aiexam.common.AjaxResult;
import com.aiexam.common.annotation.OperationLog;
import com.aiexam.common.constant.RoleConstants;
import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.RoleAddDTO;
import com.aiexam.system.dto.RoleUpdateDTO;
import com.aiexam.system.dto.RoleUserQueryDTO;
import com.aiexam.system.service.SysRoleService;
import com.aiexam.system.vo.RoleVO;
import com.aiexam.system.vo.UserVO;
import jakarta.validation.Valid;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理控制器（管理端，全部 admin）
 */
@RestController
@RequestMapping("/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    /**
     * 全量角色列表
     */
    @GetMapping("/list")
    @RequiresRoles(RoleConstants.ADMIN)
    public AjaxResult<List<RoleVO>> list() {
        return AjaxResult.success(sysRoleService.listRoles());
    }

    /**
     * 角色详情
     */
    @GetMapping("/{id}")
    @RequiresRoles(RoleConstants.ADMIN)
    public AjaxResult<RoleVO> detail(@PathVariable Long id) {
        return AjaxResult.success(sysRoleService.getRoleDetail(id));
    }

    /**
     * 角色下的用户分页
     */
    @GetMapping("/{id}/users")
    @RequiresRoles(RoleConstants.ADMIN)
    public AjaxResult<PageVO<UserVO>> roleUsers(@PathVariable Long id, RoleUserQueryDTO dto) {
        return AjaxResult.success(sysRoleService.listRoleUsers(id, dto.getPageNum(), dto.getPageSize()));
    }

    /**
     * 新增角色（编码唯一）
     */
    @PostMapping
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "角色管理", action = "新增角色")
    public AjaxResult<Long> add(@Valid @RequestBody RoleAddDTO dto) {
        return AjaxResult.success(sysRoleService.addRole(dto));
    }

    /**
     * 修改角色（编码不可修改）
     */
    @PutMapping
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "角色管理", action = "修改角色")
    public AjaxResult<Void> update(@Valid @RequestBody RoleUpdateDTO dto) {
        sysRoleService.updateRole(dto);
        return AjaxResult.success();
    }

    /**
     * 删除角色（内置角色禁删；角色下有用户禁删）
     */
    @DeleteMapping("/{id}")
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "角色管理", action = "删除角色")
    public AjaxResult<Void> delete(@PathVariable Long id) {
        sysRoleService.deleteRole(id);
        return AjaxResult.success();
    }
}
