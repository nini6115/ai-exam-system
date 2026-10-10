package com.aiexam.common.interceptor;

import com.aiexam.common.context.LoginUser;
import com.aiexam.common.context.UserContext;
import com.aiexam.common.shiro.TokenAuthenticationToken;
import com.aiexam.system.entity.SysUser;
import com.aiexam.system.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Token 鉴权拦截器
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final String TOKEN_PREFIX = "login:token:";
    private static final String HEADER_TOKEN = "Authorization";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SecurityManager securityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(HEADER_TOKEN);
        if (token == null || token.isEmpty()) {
            write401(response, "未登录");
            return false;
        }

        String userIdStr = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (userIdStr == null) {
            write401(response, "登录已过期，请重新登录");
            return false;
        }

        // 查询用户信息
        Long userId = Long.valueOf(userIdStr);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            write401(response, "账号不存在或已被禁用");
            return false;
        }

        // 放入 ThreadLocal
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRealName());
        UserContext.set(loginUser);

        // 绑定 Shiro Subject：让 @RequiresRoles 等注解在本请求生效
        // （桥接 Token 走 Realm 认证，密码比对已在登录接口完成）
        Subject subject = new Subject.Builder(securityManager).buildSubject();
        subject.login(new TokenAuthenticationToken(loginUser, token));
        ThreadContext.bind(subject);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 与 UserContext.clear 并列的双 ThreadLocal 清理（afterCompletion 异常时也保证调用）
        ThreadContext.unbindSubject();
        UserContext.clear();
    }

    /**
     * 写 401 响应
     */
    private void write401(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("msg", msg);
        result.put("data", null);
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
