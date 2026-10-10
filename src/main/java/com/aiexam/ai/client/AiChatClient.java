package com.aiexam.ai.client;

/**
 * 大模型判分客户端抽象：输入判分要素，输出结构化判分结果
 * <p>
 * 实现两个：GlmChatClient（OpenAI 兼容真实调用）、MockChatClient（确定性模拟），
 * 由配置 ai.grading.mock 决定装配哪个。
 */
public interface AiChatClient {

    /**
     * 对一道简答题判分
     *
     * @param prompt 判分要素（题目/参考答案/评分要点/满分/学生答案）
     * @return 结构化结果（score 为原始值，范围校验由调用方负责）
     * @throws RuntimeException 调用失败或返回内容无法解析为 JSON
     */
    AiGradeResult grade(AiGradePrompt prompt);
}
