package com.aiexam.system.service;

import com.aiexam.system.dto.OperLogQueryDTO;
import com.aiexam.system.entity.SysOperLog;
import com.aiexam.system.vo.OperLogVO;
import com.aiexam.common.vo.PageVO;

/**
 * 操作日志服务（异步落库 / 管理端查询）
 */
public interface SysOperLogService {

    /**
     * 异步保存操作日志（operationLogExecutor 线程池；失败仅记错误日志，不影响业务）
     *
     * @param operLog 已在切面主线程组装完整的日志实体
     */
    void saveAsync(SysOperLog operLog);

    /**
     * 管理端分页查询操作日志
     *
     * @param dto 查询参数（模块/操作人/结果码/时间范围）
     * @return 日志分页
     */
    PageVO<OperLogVO> listOperLogs(OperLogQueryDTO dto);
}
