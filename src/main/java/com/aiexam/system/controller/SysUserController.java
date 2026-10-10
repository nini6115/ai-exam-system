package com.aiexam.system.controller;

import com.aiexam.common.AjaxResult;
import com.aiexam.common.annotation.OperationLog;
import com.aiexam.common.constant.RoleConstants;
import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.AssignRoleDTO;
import com.aiexam.system.dto.ChangePasswordDTO;
import com.aiexam.system.dto.LoginDTO;
import com.aiexam.system.dto.ResetPasswordDTO;
import com.aiexam.system.dto.UpdateProfileDTO;
import com.aiexam.system.dto.UpdateStatusDTO;
import com.aiexam.system.dto.UserAddDTO;
import com.aiexam.system.dto.UserQueryDTO;
import com.aiexam.system.dto.UserUpdateDTO;
import com.aiexam.system.service.SysUserService;
import com.aiexam.system.vo.CaptchaVO;
import com.aiexam.system.vo.LoginVO;
import com.aiexam.system.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 获取图形验证码
     */
    @GetMapping("/captcha")
    public AjaxResult<CaptchaVO> captcha() {
        CaptchaVO vo = sysUserService.getCaptcha();
        return AjaxResult.success(vo);
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    @OperationLog(module = "用户管理", action = "登录")
    public AjaxResult<LoginVO> login(@Valid @RequestBody LoginDTO dto,
                                     HttpServletRequest request) {
        String clientIp = getClientIp(request);
        LoginVO vo = sysUserService.login(dto, clientIp);
        return AjaxResult.success(vo);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    @OperationLog(module = "用户管理", action = "登出")
    public AjaxResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        sysUserService.logout(token);
        return AjaxResult.success();
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    @RequiresRoles(RoleConstants.ADMIN)
    public AjaxResult<PageVO<UserVO>> list(UserQueryDTO dto) {
        return AjaxResult.success(sysUserService.listUsers(dto));
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{id}")
    @RequiresRoles(RoleConstants.ADMIN)
    public AjaxResult<UserVO> detail(@PathVariable Long id) {
        return AjaxResult.success(sysUserService.getUserDetail(id));
    }

    /**
     * 新增用户
     */
    @PostMapping
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "用户管理", action = "新增用户")
    public AjaxResult<Long> add(@Valid @RequestBody UserAddDTO dto) {
        return AjaxResult.success(sysUserService.addUser(dto));
    }

    /**
     * 修改用户
     */
    @PutMapping
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "用户管理", action = "修改用户")
    public AjaxResult<Void> update(@Valid @RequestBody UserUpdateDTO dto) {
        sysUserService.updateUser(dto);
        return AjaxResult.success();
    }

    /**
     * 删除用户（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "用户管理", action = "删除用户")
    public AjaxResult<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return AjaxResult.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/resetPwd")
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "用户管理", action = "重置密码")
    public AjaxResult<Void> resetPwd(@Valid @RequestBody ResetPasswordDTO dto) {
        sysUserService.resetPassword(dto);
        return AjaxResult.success();
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/status")
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "用户管理", action = "启用禁用用户")
    public AjaxResult<Void> updateStatus(@Valid @RequestBody UpdateStatusDTO dto) {
        sysUserService.updateStatus(dto);
        return AjaxResult.success();
    }

    /**
     * 分配角色（全量覆盖）
     */
    @PutMapping("/assignRole")
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "用户管理", action = "分配角色")
    public AjaxResult<Void> assignRole(@Valid @RequestBody AssignRoleDTO dto) {
        sysUserService.assignRole(dto);
        return AjaxResult.success();
    }

    /**
     * 修改个人资料（当前登录用户）
     */
    @PutMapping("/profile")
    @OperationLog(module = "用户管理", action = "修改个人资料")
    public AjaxResult<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        sysUserService.updateProfile(dto);
        return AjaxResult.success();
    }

    /**
     * 修改个人密码（当前登录用户）
     */
    @PutMapping("/changePwd")
    @OperationLog(module = "用户管理", action = "修改个人密码")
    public AjaxResult<Void> changePwd(@Valid @RequestBody ChangePasswordDTO dto) {
        sysUserService.changePassword(dto);
        return AjaxResult.success();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
