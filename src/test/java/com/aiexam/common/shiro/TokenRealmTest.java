package com.aiexam.common.shiro;

import com.aiexam.common.context.LoginUser;
import com.aiexam.system.entity.SysRole;
import com.aiexam.system.mapper.SysRoleMapper;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 桥接 Realm 单元测试（Mockito，不依赖 MySQL）
 */
@ExtendWith(MockitoExtension.class)
class TokenRealmTest {

    private static final Long USER_ID = 100L;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @InjectMocks
    private TokenRealm realm;

    @Test
    @DisplayName("supports：只认桥接 Token")
    void supports_onlyBridgeToken() {
        assertThat(realm.supports(new TokenAuthenticationToken(
                new LoginUser(USER_ID, "tom", "汤姆"), "token-x"))).isTrue();
        assertThat(realm.supports(new UsernamePasswordToken())).isFalse();
    }

    @Test
    @DisplayName("认证：信任桥接 Token，principal 透传 LoginUser")
    void authentication_trustsBridgeToken() {
        LoginUser loginUser = new LoginUser(USER_ID, "tom", "汤姆");
        TokenAuthenticationToken bridge = new TokenAuthenticationToken(loginUser, "token-x");

        AuthenticationInfo info = realm.getAuthenticationInfo(bridge);

        assertThat(info.getPrincipals().getPrimaryPrincipal()).isSameAs(loginUser);
    }

    @Test
    @DisplayName("授权：角色集合来自实时查库")
    void authorization_rolesFromDb() {
        LoginUser loginUser = new LoginUser(USER_ID, "tom", "汤姆");
        SysRole admin = new SysRole();
        admin.setRoleCode("admin");
        SysRole teacher = new SysRole();
        teacher.setRoleCode("teacher");
        when(sysRoleMapper.selectByUserId(USER_ID)).thenReturn(List.of(admin, teacher));

        AuthorizationInfo info = realm.doGetAuthorizationInfo(
                new SimplePrincipalCollection(loginUser, realm.getName()));

        assertThat(info.getRoles()).containsExactlyInAnyOrder("admin", "teacher");
    }

    @Test
    @DisplayName("授权：无角色返回空集合（不返回 null）")
    void authorization_noRoles_emptySet() {
        LoginUser loginUser = new LoginUser(USER_ID, "tom", "汤姆");
        when(sysRoleMapper.selectByUserId(USER_ID)).thenReturn(List.of());

        AuthorizationInfo info = realm.doGetAuthorizationInfo(
                new SimplePrincipalCollection(loginUser, realm.getName()));

        assertThat(info.getRoles()).isEmpty();
    }
}
