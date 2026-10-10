package com.aiexam.system.controller;

import com.aiexam.common.AjaxResult;
import com.aiexam.common.constant.RoleConstants;
import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.OperLogQueryDTO;
import com.aiexam.system.service.SysOperLogService;
import com.aiexam.system.vo.OperLogVO;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志控制器（管理端查询）
 */
@RestController
@RequestMapping("/system/log")
public class SysOperLogController {

    @Autowired
    private SysOperLogService sysOperLogService;

    /**
     * 操作日志分页查询（模块/操作人/结果码/时间范围筛选）
     */
    @GetMapping("/list")
    @RequiresRoles(RoleConstants.ADMIN)
    public AjaxResult<PageVO<OperLogVO>> list(OperLogQueryDTO dto) {
        return AjaxResult.success(sysOperLogService.listOperLogs(dto));
    }
}
