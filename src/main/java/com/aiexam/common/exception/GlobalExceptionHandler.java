package com.aiexam.common.exception;

import com.aiexam.common.AjaxResult;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Shiro 鉴权失败（UnauthorizedException 是其子类）——规范错误码 403
     */
    @ExceptionHandler(AuthorizationException.class)
    public AjaxResult<Void> handleAuthorizationException(AuthorizationException e) {
        // 固定文案，不透出 Shiro 内部消息
        return AjaxResult.error(403, "无权限访问");
    }

    /**
     * Shiro 未认证（防御性：正常链路 401 已被 TokenInterceptor 前置拦截）
     */
    @ExceptionHandler(UnauthenticatedException.class)
    public AjaxResult<Void> handleUnauthenticatedException(UnauthenticatedException e) {
        return AjaxResult.error(401, "未登录或登录已过期");
    }

    /**
     * 业务异常（RuntimeException 直接当业务异常用）
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult<Void> handleRuntimeException(RuntimeException e) {
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 参数校验异常 - @RequestBody + @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return AjaxResult.error(400, msg);
    }

    /**
     * 参数校验异常 - 表单绑定
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return AjaxResult.error(400, msg);
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult<Void> handleException(Exception e) {
        return AjaxResult.error(500, "系统异常：" + e.getMessage());
    }
}
