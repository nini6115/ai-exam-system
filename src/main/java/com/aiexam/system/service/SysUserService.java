package com.aiexam.system.service;

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
import com.aiexam.system.entity.SysUser;
import com.aiexam.system.vo.CaptchaVO;
import com.aiexam.system.vo.LoginVO;
import com.aiexam.system.vo.UserVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 获取图形验证码
     */
    CaptchaVO getCaptcha();

    /**
     * 登录
     *
     * @param dto      登录参数
     * @param clientIp 客户端IP
     * @return 登录结果
     */
    LoginVO login(LoginDTO dto, String clientIp);

    /**
     * 登出
     *
     * @param token 用户token
     */
    void logout(String token);

    /**
     * 分页查询用户列表
     */
    PageVO<UserVO> listUsers(UserQueryDTO dto);

    /**
     * 查询用户详情（含角色）
     */
    UserVO getUserDetail(Long id);

    /**
     * 新增用户
     *
     * @return 新用户ID
     */
    Long addUser(UserAddDTO dto);

    /**
     * 修改用户基本信息（roleIds 传了就全量覆盖）
     */
    void updateUser(UserUpdateDTO dto);

    /**
     * 删除用户（逻辑删除）
     */
    void deleteUser(Long id);

    /**
     * 管理员重置用户密码（重置后强制下线）
     */
    void resetPassword(ResetPasswordDTO dto);

    /**
     * 启用/禁用用户（禁用后强制下线）
     */
    void updateStatus(UpdateStatusDTO dto);

    /**
     * 给用户分配角色（全量覆盖）
     */
    void assignRole(AssignRoleDTO dto);

    /**
     * 修改个人资料（当前登录用户）
     */
    void updateProfile(UpdateProfileDTO dto);

    /**
     * 修改个人密码（当前登录用户，成功后强制下线）
     */
    void changePassword(ChangePasswordDTO dto);
}
