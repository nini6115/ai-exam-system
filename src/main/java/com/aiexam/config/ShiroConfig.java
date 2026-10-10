package com.aiexam.config;

import com.aiexam.common.shiro.TokenRealm;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shiro 装配（共存模式）
 * <p>
 * 登录认证仍由 TokenInterceptor 负责（Redis token），Shiro 只承担接口级
 * {@code @RequiresRoles} 鉴权注解。故用非 web 的 DefaultSecurityManager、
 * 不引 web starter 自动装配（无过滤链/无 web 会话/零 yml）。
 */
@Configuration
public class ShiroConfig {

    @Bean
    public DefaultSecurityManager securityManager(TokenRealm tokenRealm) {
        DefaultSecurityManager securityManager = new DefaultSecurityManager(tokenRealm);
        // 每请求新建 Subject 即弃，禁用 Session 持久化（避免每次 login 白建内存 Session）
        DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        securityManager.setSubjectDAO(subjectDAO);
        return securityManager;
    }

    /**
     * 让 @RequiresRoles 等 Shiro 注解生效
     * <p>
     * order=2：晚于 OperationLogAspect(@Order(1))——鉴权失败也记操作日志
     * （安全审计需要留越权尝试痕迹，result_code=403）。
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(
            org.apache.shiro.mgt.SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        advisor.setOrder(2);
        return advisor;
    }

    /**
     * static：BeanPostProcessor 需提前初始化，避免连带过早实例化 Mapper 等依赖
     */
    @Bean
    public static LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }
}
