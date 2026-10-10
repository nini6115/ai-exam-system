package com.aiexam.system.mapper;

import com.aiexam.system.entity.SysRole;
import com.aiexam.system.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色列表
     */
    List<SysRole> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID列表批量查询角色（结果携带 userId）
     */
    List<SysRole> selectByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 统计角色下已分配的用户数（删角色守卫）
     *
     * @param roleId 角色ID
     * @return 用户数
     */
    long countUsersByRoleId(@Param("roleId") Long roleId);

    /**
     * 分页查询角色下的用户列表（join sys_user，SQL 在 XML 中）
     *
     * @param page   分页对象
     * @param roleId 角色ID
     * @return 用户分页
     */
    IPage<SysUser> selectUsersByRoleId(Page<SysUser> page, @Param("roleId") Long roleId);
}
