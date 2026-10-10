package com.aiexam.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 * <p>
 * 注意：sys_oper_log 表只插入不更新，没有 update_time / deleted 字段，故不继承 BaseEntity。
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块名 */
    private String module;

    /** 操作名 */
    private String action;

    /** 操作人ID（登录等白名单场景为 null） */
    private Long operatorId;

    /** 操作人用户名快照 */
    private String operatorName;

    /** 操作IP */
    private String operatorIp;

    /** 请求方式 */
    private String requestMethod;

    /** 请求URI */
    private String requestUri;

    /** 请求参数JSON（敏感字段已脱敏，超长截断） */
    private String params;

    /** 结果码：200成功 500失败 */
    private Integer resultCode;

    /** 失败原因 */
    private String errorMsg;

    /** 耗时毫秒 */
    private Long costMs;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
