package com.aiexam.common.aspect;

import com.aiexam.common.annotation.OperationLog;
import com.aiexam.common.context.LoginUser;
import com.aiexam.common.context.UserContext;
import com.aiexam.common.utils.IpUtil;
import com.aiexam.common.utils.LogParamSerializer;
import com.aiexam.system.entity.SysOperLog;
import com.aiexam.system.service.SysOperLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.AuthorizationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面：拦截 @OperationLog 标注的方法，成功/失败均记录（异步落库）
 * <p>
 * 顺序即正确性：UserContext 与 RequestContextHolder 都是 ThreadLocal，必须在切面主线程
 * 取齐用户与请求信息后组装完整实体，异步线程只做 insert。
 * <p>
 * 已知限制：@Valid 参数校验失败（400）发生在 Controller 方法调用前，不经过本切面，
 * 此类参数抖动不记录——操作日志只记用户操作，不记参数错误。
 */
@Slf4j
@Order(1)
@Aspect
@Component
public class OperationLogAspect {

    /** 参数 JSON 最大长度（与表列宽匹配，TEXT 但控制体积） */
    private static final int PARAMS_MAX_LENGTH = 2000;
    /** 失败原因最大长度（error_msg VARCHAR(500)） */
    private static final int ERROR_MSG_MAX_LENGTH = 500;
    /** 结果码：与 AjaxResult 一致 */
    private static final int RESULT_SUCCESS = 200;
    private static final int RESULT_ERROR = 500;
    /** Shiro 鉴权失败（与全局异常处理器的 403 对齐，越权尝试也留审计痕迹） */
    private static final int RESULT_FORBIDDEN = 403;

    @Autowired
    private SysOperLogService sysOperLogService;

    /**
     * 环绕通知：业务异常原样上抛（GlobalExceptionHandler 照常生效），日志失败只吞不抛
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startNanos = System.nanoTime();
        // 主线程先取上下文（异步线程取不到这些 ThreadLocal）
        LoginUser operator = UserContext.get();
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ip = null;
        String requestMethod = null;
        String requestUri = null;
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ip = IpUtil.getIp(request);
            requestMethod = request.getMethod();
            requestUri = request.getRequestURI();
        }

        try {
            Object result = joinPoint.proceed();
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            saveLog(joinPoint, operationLog, operator, ip, requestMethod, requestUri,
                    RESULT_SUCCESS, null, costMs);
            return result;
        } catch (AuthorizationException e) {
            // Shiro 鉴权失败：审计码与 API 返回码一致（403），异常原样上抛交由全局处理器响应
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            saveLog(joinPoint, operationLog, operator, ip, requestMethod, requestUri,
                    RESULT_FORBIDDEN, e.getMessage(), costMs);
            throw e;
        } catch (Throwable e) {
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            saveLog(joinPoint, operationLog, operator, ip, requestMethod, requestUri,
                    RESULT_ERROR, e.getMessage(), costMs);
            // 原样上抛，不改变既有异常处理链路
            throw e;
        }
    }

    /**
     * 组装并异步保存日志：整体兜底，任何失败仅记错误日志，绝不影响业务
     */
    private void saveLog(ProceedingJoinPoint joinPoint, OperationLog operationLog,
                         LoginUser operator, String ip, String requestMethod, String requestUri,
                         int resultCode, String errorMsg, long costMs) {
        try {
            SysOperLog operLog = new SysOperLog();
            operLog.setModule(operationLog.module());
            operLog.setAction(operationLog.action());
            operLog.setOperatorId(operator != null ? operator.getUserId() : null);
            operLog.setOperatorName(operator != null ? operator.getUsername() : null);
            operLog.setOperatorIp(ip);
            operLog.setRequestMethod(requestMethod);
            operLog.setRequestUri(requestUri);
            if (operationLog.recordParam()) {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                operLog.setParams(LogParamSerializer.serialize(
                        signature.getParameterNames(), joinPoint.getArgs(), PARAMS_MAX_LENGTH));
            }
            operLog.setResultCode(resultCode);
            operLog.setErrorMsg(errorMsg == null ? null
                    : (errorMsg.length() <= ERROR_MSG_MAX_LENGTH
                    ? errorMsg : errorMsg.substring(0, ERROR_MSG_MAX_LENGTH)));
            operLog.setCostMs(costMs);
            sysOperLogService.saveAsync(operLog);
        } catch (Exception ex) {
            log.error("操作日志记录失败，module[{}] action[{}]",
                    operationLog.module(), operationLog.action(), ex);
        }
    }
}
