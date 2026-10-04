package com.aiexam.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 验证码返回
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 验证码Key */
    private String captchaKey;

    /** base64图片 */
    private String captchaImage;
}
