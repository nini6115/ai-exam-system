package com.aiexam.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在需要审计的写操作 Controller 方法上，
 * 由 OperationLogAspect 环绕切面自动记录操作人/入参/结果/耗时（异步落库）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 模块名，如「用户管理」 */
    String module();

    /** 操作名，如「新增用户」 */
    String action();

    /** 是否记录入参（大参数接口可显式关闭） */
    boolean recordParam() default true;
}
