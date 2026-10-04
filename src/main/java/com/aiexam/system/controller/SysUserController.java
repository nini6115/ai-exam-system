package com.aiexam.system.controller;

import com.aiexam.common.AjaxResult;
import com.aiexam.system.dto.LoginDTO;
import com.aiexam.system.service.SysUserService;
import com.aiexam.system.vo.CaptchaVO;
import com.aiexam.system.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 获取图形验证码
     */
    @GetMapping("/captcha")
    public AjaxResult<CaptchaVO> captcha() {
        CaptchaVO vo = sysUserService.getCaptcha();
        return AjaxResult.success(vo);
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public AjaxResult<LoginVO> login(@Valid @RequestBody LoginDTO dto,
                                     HttpServletRequest request) {
        String clientIp = getClientIp(request);
        LoginVO vo = sysUserService.login(dto, clientIp);
        return AjaxResult.success(vo);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public AjaxResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        sysUserService.logout(token);
        return AjaxResult.success();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
