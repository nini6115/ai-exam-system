package com.aiexam.system.mapper;

import com.aiexam.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper {

    /**
     * 根据用户名查询用户（含角色列表）
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据ID查询用户
     */
    SysUser selectById(@Param("id") Long id);

    /**
     * 更新登录信息
     */
    int updateLoginInfo(@Param("id") Long id,
                        @Param("lastLoginTime") LocalDateTime lastLoginTime,
                        @Param("lastLoginIp") String lastLoginIp);
}
