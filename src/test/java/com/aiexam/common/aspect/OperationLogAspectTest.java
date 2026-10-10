package com.aiexam.common.aspect;

import com.aiexam.common.annotation.OperationLog;
import com.aiexam.common.context.LoginUser;
import com.aiexam.common.context.UserContext;
import com.aiexam.system.entity.SysOperLog;
import com.aiexam.system.service.SysOperLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 操作日志切面单元测试（Mockito，不依赖 MySQL/网络）
 * <p>
 * 直接调用切面方法（不启动 AOP 代理），注解与连接点均为 mock。
 */
@ExtendWith(MockitoExtension.class)
class OperationLogAspectTest {

    private static final Long TEACHER_ID = 1L;

    @Mock
    private SysOperLogService sysOperLogService;

    @InjectMocks
    private OperationLogAspect aspect;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/user/add");
        request.setRemoteAddr("192.168.1.2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        UserContext.set(new LoginUser(TEACHER_ID, "teacher01", "王老师"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        UserContext.clear();
    }

    @Test
    @DisplayName("成功路径：返回原值，记录 200/操作人/IP/URI/耗时/参数")
    void around_success_logsFullEntity() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(new String[]{"dto"}, new Object[]{"参数值"}, "ok");
        OperationLog operationLog = annotation(true);

        Object result = aspect.around(joinPoint, operationLog);

        assertThat(result).isEqualTo("ok");
        ArgumentCaptor<SysOperLog> captor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(sysOperLogService).saveAsync(captor.capture());
        SysOperLog entity = captor.getValue();
        assertThat(entity.getModule()).isEqualTo("用户管理");
        assertThat(entity.getAction()).isEqualTo("新增用户");
        assertThat(entity.getResultCode()).isEqualTo(200);
        assertThat(entity.getErrorMsg()).isNull();
        assertThat(entity.getOperatorId()).isEqualTo(TEACHER_ID);
        assertThat(entity.getOperatorName()).isEqualTo("teacher01");
        assertThat(entity.getOperatorIp()).isEqualTo("192.168.1.2");
        assertThat(entity.getRequestMethod()).isEqualTo("POST");
        assertThat(entity.getRequestUri()).isEqualTo("/user/add");
        assertThat(entity.getParams()).contains("参数值");
        assertThat(entity.getCostMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("异常路径：记录 500/失败原因，且异常原样上抛")
    void around_exception_logsAndRethrows() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(new String[0], new Object[0], null);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("业务失败"));
        OperationLog operationLog = annotation(true);

        assertThatThrownBy(() -> aspect.around(joinPoint, operationLog))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("业务失败");

        ArgumentCaptor<SysOperLog> captor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(sysOperLogService).saveAsync(captor.capture());
        assertThat(captor.getValue().getResultCode()).isEqualTo(500);
        assertThat(captor.getValue().getErrorMsg()).isEqualTo("业务失败");
    }

    @Test
    @DisplayName("匿名场景（登录等白名单接口）：操作人为 null")
    void around_anonymous_operatorNull() throws Throwable {
        UserContext.clear();
        ProceedingJoinPoint joinPoint = joinPoint(new String[0], new Object[0], null);
        OperationLog operationLog = annotation(true);

        aspect.around(joinPoint, operationLog);

        ArgumentCaptor<SysOperLog> captor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(sysOperLogService).saveAsync(captor.capture());
        assertThat(captor.getValue().getOperatorId()).isNull();
        assertThat(captor.getValue().getOperatorName()).isNull();
    }

    @Test
    @DisplayName("切面兜底：日志服务抛异常不影响业务返回值")
    void around_saveFailure_businessUnaffected() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(new String[0], new Object[0], null);
        OperationLog operationLog = annotation(true);
        doThrow(new RuntimeException("模拟线程池拒绝")).when(sysOperLogService).saveAsync(any(SysOperLog.class));

        Object result = aspect.around(joinPoint, operationLog);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("recordParam=false：不记录参数")
    void around_recordParamFalse_paramsNull() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(new String[]{"dto"}, new Object[]{"参数值"}, null);
        OperationLog operationLog = annotation(false);

        aspect.around(joinPoint, operationLog);

        ArgumentCaptor<SysOperLog> captor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(sysOperLogService).saveAsync(captor.capture());
        assertThat(captor.getValue().getParams()).isNull();
    }

    @Test
    @DisplayName("失败原因超长截断到 500")
    void around_errorMsgTruncated() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(new String[0], new Object[0], null);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("败".repeat(600)));
        OperationLog operationLog = annotation(true);

        assertThatThrownBy(() -> aspect.around(joinPoint, operationLog))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<SysOperLog> captor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(sysOperLogService).saveAsync(captor.capture());
        assertThat(captor.getValue().getErrorMsg()).hasSize(500);
    }

    // ==================== 测试数据 ====================

    private ProceedingJoinPoint joinPoint(String[] paramNames, Object[] args, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        // lenient：recordParam=false 时参数相关桩不会被消耗
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.getParameterNames()).thenReturn(paramNames);
        lenient().when(joinPoint.getArgs()).thenReturn(args);
        // result=null 时由调用方另行 stub 异常路径
        if (result != null) {
            when(joinPoint.proceed()).thenReturn(result);
        }
        return joinPoint;
    }

    private OperationLog annotation(boolean recordParam) {
        OperationLog operationLog = mock(OperationLog.class);
        when(operationLog.module()).thenReturn("用户管理");
        when(operationLog.action()).thenReturn("新增用户");
        when(operationLog.recordParam()).thenReturn(recordParam);
        return operationLog;
    }
}
