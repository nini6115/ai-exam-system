package com.aiexam.system.service;

import com.aiexam.system.dto.LoginDTO;
import com.aiexam.system.vo.CaptchaVO;
import com.aiexam.system.vo.LoginVO;

/**
 * 用户服务接口
 */
public interface SysUserService {

    /**
     * 获取图形验证码
     */
    CaptchaVO getCaptcha();

    /**
     * 登录
     *
     * @param dto      登录参数
     * @param clientIp 客户端IP
     * @return 登录结果
     */
    LoginVO login(LoginDTO dto, String clientIp);

    /**
     * 登出
     *
     * @param token 用户token
     */
    void logout(String token);
}
