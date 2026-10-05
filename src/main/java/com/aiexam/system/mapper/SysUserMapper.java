package com.aiexam.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiexam.system.dto.UserQueryDTO;
import com.aiexam.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户（含角色列表）
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 分页查询用户列表（多条件，不含角色，角色由 service 批量填充）
     */
    Page<SysUser> selectUserPage(Page<SysUser> page, @Param("q") UserQueryDTO query);

    /**
     * 批量插入用户角色关联
     */
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 删除用户所有角色关联
     */
    int deleteUserRoles(@Param("userId") Long userId);

    /**
     * 更新登录信息
     */
    int updateLoginInfo(@Param("id") Long id,
                        @Param("lastLoginTime") LocalDateTime lastLoginTime,
                        @Param("lastLoginIp") String lastLoginIp);
}
