package com.aiexam.system.service.impl;

import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.OperLogQueryDTO;
import com.aiexam.system.entity.SysOperLog;
import com.aiexam.system.mapper.SysOperLogMapper;
import com.aiexam.system.service.SysOperLogService;
import com.aiexam.system.vo.OperLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现
 * <p>
 * 无自有 CRUD 面（只 insert + 条件分页），不继承 ServiceImpl。
 * saveAsync 由切面跨 Bean 调用（走代理），@Async 生效无自调用陷阱。
 */
@Slf4j
@Service
public class SysOperLogServiceImpl implements SysOperLogService {

    /** 每页最大条数 */
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private SysOperLogMapper sysOperLogMapper;

    @Async("operationLogExecutor")
    @Override
    public void saveAsync(SysOperLog operLog) {
        try {
            sysOperLogMapper.insert(operLog);
        } catch (Exception e) {
            // @Async void：异常吞掉，仅记错误日志，绝不影响业务
            log.error("操作日志落库失败，module[{}] action[{}]", operLog.getModule(), operLog.getAction(), e);
        }
    }

    @Override
    public PageVO<OperLogVO> listOperLogs(OperLogQueryDTO dto) {
        Page<SysOperLog> page = new Page<>(dto.getPageNum(),
                Math.min(dto.getPageSize(), MAX_PAGE_SIZE));
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<SysOperLog>()
                .eq(dto.getModule() != null && !dto.getModule().isEmpty(),
                        SysOperLog::getModule, dto.getModule())
                .like(dto.getOperatorName() != null && !dto.getOperatorName().isEmpty(),
                        SysOperLog::getOperatorName, dto.getOperatorName())
                .eq(dto.getResultCode() != null, SysOperLog::getResultCode, dto.getResultCode())
                .ge(dto.getStartTime() != null, SysOperLog::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, SysOperLog::getCreateTime, dto.getEndTime())
                .orderByDesc(SysOperLog::getCreateTime)
                .orderByDesc(SysOperLog::getId);
        Page<SysOperLog> result = sysOperLogMapper.selectPage(page, wrapper);
        List<OperLogVO> vos = result.getRecords().stream().map(OperLogVO::from).toList();
        return PageVO.of(page, vos);
    }
}
