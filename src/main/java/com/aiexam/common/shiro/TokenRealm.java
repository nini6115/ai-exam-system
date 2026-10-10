package com.aiexam.common.shiro;

import com.aiexam.common.context.LoginUser;
import com.aiexam.system.entity.SysRole;
import com.aiexam.system.mapper.SysRoleMapper;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 桥接 Realm：认证直接信任 TokenInterceptor 已完成的 Redis token 校验（零 IO 透传），
 * 授权按 userId 实时查角色（不配 CacheManager——每请求一次 selectByUserId 与迁移前
 * 成本一致，角色分配/删除即时生效，无缓存失效问题）
 */
@Component
public class TokenRealm extends AuthorizingRealm {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    public TokenRealm() {
        // 密码校验已在登录接口完成，桥接 Token 直接放行
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        // 只认桥接 Token，不与任何其他认证方式混淆
        return token instanceof TokenAuthenticationToken;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        return new SimpleAuthenticationInfo(
                ((TokenAuthenticationToken) token).getLoginUser(),
                token.getCredentials(), getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        LoginUser user = (LoginUser) principals.getPrimaryPrincipal();
        Set<String> roleCodes = sysRoleMapper.selectByUserId(user.getUserId()).stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toSet());
        // 空角色也返回空 info（不能返回 null，否则视为无授权信息反复触发查询）
        return new SimpleAuthorizationInfo(roleCodes);
    }
}
