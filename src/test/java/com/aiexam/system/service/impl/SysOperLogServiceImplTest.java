package com.aiexam.system.service.impl;

import com.aiexam.common.context.LoginUser;
import com.aiexam.common.context.UserContext;
import com.aiexam.system.entity.SysOperLog;
import com.aiexam.system.mapper.SysOperLogMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 操作日志服务单元测试（Mockito，不依赖 MySQL）
 */
@ExtendWith(MockitoExtension.class)
class SysOperLogServiceImplTest {

    private static final Long ADMIN_ID = 1L;

    @Mock
    private SysOperLogMapper sysOperLogMapper;

    @InjectMocks
    private SysOperLogServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        // LambdaQueryWrapper 解析列需要 MP 的 TableInfo 缓存
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysOperLog.class);
    }

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(ADMIN_ID, "admin", "系统管理员"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("查询：pageSize 钳制到 100 并正常分页")
    void listOperLogs_pageSizeClamped() {
        when(sysOperLogMapper.selectPage(any(Page.class), any(Wrapper.class)))
                .thenAnswer(invocation -> {
                    Page<SysOperLog> page = invocation.getArgument(0);
                    page.setRecords(List.of(buildLog()));
                    page.setTotal(1);
                    return page;
                });
        com.aiexam.system.dto.OperLogQueryDTO dto = new com.aiexam.system.dto.OperLogQueryDTO();
        dto.setPageSize(500);

        var vo = service.listOperLogs(dto);

        assertThat(vo.getList()).hasSize(1);
        assertThat(vo.getList().get(0).getModule()).isEqualTo("用户管理");
        ArgumentCaptor<Page<SysOperLog>> captor = ArgumentCaptor.forClass(Page.class);
        verify(sysOperLogMapper).selectPage(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("异步落库：insert 抛异常不向外传播")
    void saveAsync_insertFailure_swallowed() {
        doThrow(new RuntimeException("模拟数据库不可用"))
                .when(sysOperLogMapper).insert(any(SysOperLog.class));

        service.saveAsync(buildLog());

        // 未抛异常即通过（@Async void 语义：异常吞掉仅记日志）
        verify(sysOperLogMapper).insert(any(SysOperLog.class));
    }

    private SysOperLog buildLog() {
        SysOperLog log = new SysOperLog();
        log.setModule("用户管理");
        log.setAction("新增用户");
        log.setResultCode(200);
        return log;
    }
}
