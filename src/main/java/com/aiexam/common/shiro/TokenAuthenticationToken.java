package com.aiexam.common.shiro;

import com.aiexam.common.context.LoginUser;
import org.apache.shiro.authc.AuthenticationToken;

/**
 * 桥接 Token：把 TokenInterceptor 已验证的 Redis token 桥接给 Shiro
 * <p>
 * principal = 登录用户信息（授权时按 userId 查角色）；credentials = 原始 token 串。
 * 密码比对已在 SysUserService.login 用 BCrypt 完成，Realm 侧直接放行。
 */
public class TokenAuthenticationToken implements AuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final LoginUser loginUser;

    private final String token;

    public TokenAuthenticationToken(LoginUser loginUser, String token) {
        this.loginUser = loginUser;
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return loginUser;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    public LoginUser getLoginUser() {
        return loginUser;
    }
}
