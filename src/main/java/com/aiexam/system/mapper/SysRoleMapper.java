package com.aiexam.system.mapper;

import com.aiexam.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper
 */
@Mapper
public interface SysRoleMapper {

    /**
     * 根据用户ID查询角色列表
     */
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}
