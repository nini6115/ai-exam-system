package com.aiexam.system.mapper;

import com.aiexam.system.entity.SysOperLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper（单表 CRUD 全部由 MyBatis-Plus 提供）
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
