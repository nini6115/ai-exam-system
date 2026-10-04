package com.aiexam.system.service.impl;

import com.aiexam.common.utils.CaptchaUtil;
import com.aiexam.system.dto.LoginDTO;
import com.aiexam.system.entity.SysUser;
import com.aiexam.system.mapper.SysUserMapper;
import com.aiexam.system.service.SysUserService;
import com.aiexam.system.vo.CaptchaVO;
import com.aiexam.system.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    /** 验证码 Redis key 前缀 */
    private static final String CAPTCHA_PREFIX = "login:captcha:";
    /** Token Redis key 前缀 */
    private static final String TOKEN_PREFIX = "login:token:";
    /** 验证码有效期（秒） */
    private static final long CAPTCHA_EXPIRE = 120;
    /** Token 有效期（秒）- 2小时 */
    private static final long TOKEN_EXPIRE = 7200;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public CaptchaVO getCaptcha() {
        CaptchaUtil.CaptchaResult result = CaptchaUtil.generate();
        String key = UUID.randomUUID().toString().replace("-", "");
        // 存 Redis，转小写存，比对时也转小写
        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + key,
                result.getCode().toLowerCase(), CAPTCHA_EXPIRE, TimeUnit.SECONDS);
        return new CaptchaVO(key, result.getImageBase64());
    }

    @Override
    public LoginVO login(LoginDTO dto, String clientIp) {
        // 1. 校验验证码
        String captchaKey = CAPTCHA_PREFIX + dto.getCaptchaKey();
        String savedCode = redisTemplate.opsForValue().get(captchaKey);
        if (savedCode == null) {
            throw new RuntimeException("验证码已过期");
        }
        // 验证一次即删除
        redisTemplate.delete(captchaKey);
        if (!savedCode.equalsIgnoreCase(dto.getCaptchaCode())) {
            throw new RuntimeException("验证码错误");
        }

        // 2. 查询用户
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 校验状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        // 4. 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 5. 生成 Token
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token,
                String.valueOf(user.getId()), TOKEN_EXPIRE, TimeUnit.SECONDS);

        // 6. 更新登录信息
        sysUserMapper.updateLoginInfo(user.getId(), LocalDateTime.now(), clientIp);

        // 7. 组装返回
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(user.getRoles());
        return vo;
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            redisTemplate.delete(TOKEN_PREFIX + token);
        }
    }
}
