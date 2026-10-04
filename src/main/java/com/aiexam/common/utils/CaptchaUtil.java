package com.aiexam.common.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

/**
 * 图形验证码工具类
 * 纯 Java2D 实现，不依赖第三方库
 */
public class CaptchaUtil {

    /** 验证码字符集（排除 0/O/1/I 易混淆） */
    private static final char[] CHARS = {
            '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LEN = 4;
    private static final int LINE_COUNT = 5;

    /**
     * 生成验证码，返回 {code, imageBase64}
     */
    public static CaptchaResult generate() {
        Random random = new Random();

        // 生成随机码
        char[] code = new char[CODE_LEN];
        for (int i = 0; i < CODE_LEN; i++) {
            code[i] = CHARS[random.nextInt(CHARS.length)];
        }
        String codeStr = new String(code);

        // 画图
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        for (int i = 0; i < LINE_COUNT; i++) {
            g.setColor(randomColor(random, 150, 200));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 干扰点
        for (int i = 0; i < 30; i++) {
            g.setColor(randomColor(random, 150, 220));
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }

        // 画字符
        g.setFont(new Font("Arial", Font.BOLD, 28));
        int charWidth = WIDTH / CODE_LEN;
        for (int i = 0; i < CODE_LEN; i++) {
            g.setColor(randomColor(random, 30, 120));
            // 轻微旋转
            int x = i * charWidth + 6;
            int y = HEIGHT - 10 + random.nextInt(6);
            g.drawString(String.valueOf(code[i]), x, y);
        }

        g.dispose();

        // 转 base64
        String base64;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("验证码生成失败", e);
        }

        return new CaptchaResult(codeStr, base64);
    }

    private static Color randomColor(Random random, int min, int max) {
        int r = min + random.nextInt(max - min);
        int g = min + random.nextInt(max - min);
        int b = min + random.nextInt(max - min);
        return new Color(r, g, b);
    }

    public static class CaptchaResult {
        private final String code;
        private final String imageBase64;

        public CaptchaResult(String code, String imageBase64) {
            this.code = code;
            this.imageBase64 = imageBase64;
        }

        public String getCode() { return code; }
        public String getImageBase64() { return imageBase64; }
    }
}
