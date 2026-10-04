package com.aiexam.common.exception;

import com.aiexam.common.AjaxResult;
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
