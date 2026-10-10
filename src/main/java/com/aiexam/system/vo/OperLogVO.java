package com.aiexam.system.vo;

import com.aiexam.system.entity.SysOperLog;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志VO
 */
@Data
public class OperLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long id;

    /** 模块名 */
    private String module;

    /** 操作名 */
    private String action;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人用户名 */
    private String operatorName;

    /** 操作IP */
    private String operatorIp;

    /** 请求方式 */
    private String requestMethod;

    /** 请求URI */
    private String requestUri;

    /** 请求参数JSON（敏感字段已脱敏） */
    private String params;

    /** 结果码：200成功 500失败 */
    private Integer resultCode;

    /** 失败原因 */
    private String errorMsg;

    /** 耗时毫秒 */
    private Long costMs;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public static OperLogVO from(SysOperLog entity) {
        OperLogVO vo = new OperLogVO();
        vo.setId(entity.getId());
        vo.setModule(entity.getModule());
        vo.setAction(entity.getAction());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setOperatorIp(entity.getOperatorIp());
        vo.setRequestMethod(entity.getRequestMethod());
        vo.setRequestUri(entity.getRequestUri());
        vo.setParams(entity.getParams());
        vo.setResultCode(entity.getResultCode());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCostMs(entity.getCostMs());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
