package com.aiexam.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aiexam.system.entity.SysRole;
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
}
