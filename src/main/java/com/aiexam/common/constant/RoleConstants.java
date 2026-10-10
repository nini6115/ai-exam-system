package com.aiexam.common.constant;

/**
 * 角色编码常量（V1 预置三角色；@RequiresRoles 注解要求编译期常量，故独立成类）
 */
public final class RoleConstants {

    /** 超级管理员 */
    public static final String ADMIN = "admin";

    /** 教师 */
    public static final String TEACHER = "teacher";

    /** 学生 */
    public static final String STUDENT = "student";

    /** 内置角色集合（禁删），无序判断用 */
    public static boolean isBuiltin(String roleCode) {
        return ADMIN.equals(roleCode) || TEACHER.equals(roleCode) || STUDENT.equals(roleCode);
    }

    private RoleConstants() {
    }
}
