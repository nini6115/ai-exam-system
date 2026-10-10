package com.aiexam.common.shiro;

import com.aiexam.ai.controller.AiGradingController;
import com.aiexam.analysis.controller.AnalysisController;
import com.aiexam.exam.controller.ExamController;
import com.aiexam.system.controller.DictDataController;
import com.aiexam.system.controller.DictTypeController;
import com.aiexam.system.controller.SysOperLogController;
import com.aiexam.system.controller.SysRoleController;
import com.aiexam.system.controller.SysUserController;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权限点迁移守护网：反射断言各 Controller 方法的 @RequiresRoles 覆盖与角色值。
 * <p>
 * 断言值用字面量（"admin"/"teacher"/"student"）而非 RoleConstants——常量漂移也能被测出。
 * 多角色必须显式 Logical.OR（@RequiresRoles 默认 AND，会收紧到无人可过）。
 */
class PermissionAnnotationTest {

    // ==================== 考试管理 ====================

    @Test
    @DisplayName("考试管理：教师/管理端点与我的大厅")
    void examController() {
        assertRoles(ExamController.class, "publish", "admin", "teacher");
        assertRoles(ExamController.class, "list", "admin", "teacher");
        assertRoles(ExamController.class, "detail", "admin", "teacher");
        assertRoles(ExamController.class, "myList", "student");
        // 学生答题四接口为业务校验，非角色权限
        assertNoRoles(ExamController.class, "start");
        assertNoRoles(ExamController.class, "save");
        assertNoRoles(ExamController.class, "submit");
        assertNoRoles(ExamController.class, "cheat");
    }

    // ==================== 成绩分析 / AI 判卷 ====================

    @Test
    @DisplayName("成绩分析：三个端点教师/管理")
    void analysisController() {
        assertRoles(AnalysisController.class, "stats", "admin", "teacher");
        assertRoles(AnalysisController.class, "scores", "admin", "teacher");
        assertRoles(AnalysisController.class, "export", "admin", "teacher");
    }

    @Test
    @DisplayName("AI 判卷：三个端点教师/管理")
    void aiGradingController() {
        assertRoles(AiGradingController.class, "grade", "admin", "teacher");
        assertRoles(AiGradingController.class, "sheetDetails", "admin", "teacher");
        assertRoles(AiGradingController.class, "manualGrade", "admin", "teacher");
    }

    // ==================== 系统管理 ====================

    @Test
    @DisplayName("操作日志：admin")
    void sysOperLogController() {
        assertRoles(SysOperLogController.class, "list", "admin");
    }

    @Test
    @DisplayName("字典类型：六个端点全部 admin")
    void dictTypeController() {
        assertRoles(DictTypeController.class, "page", "admin");
        assertRoles(DictTypeController.class, "list", "admin");
        assertRoles(DictTypeController.class, "detail", "admin");
        assertRoles(DictTypeController.class, "add", "admin");
        assertRoles(DictTypeController.class, "update", "admin");
        assertRoles(DictTypeController.class, "delete", "admin");
    }

    @Test
    @DisplayName("字典数据：五个端点 admin，按类型查询登录即可")
    void dictDataController() {
        assertRoles(DictDataController.class, "page", "admin");
        assertRoles(DictDataController.class, "detail", "admin");
        assertRoles(DictDataController.class, "add", "admin");
        assertRoles(DictDataController.class, "update", "admin");
        assertRoles(DictDataController.class, "delete", "admin");
        assertNoRoles(DictDataController.class, "listByType");
    }

    // ==================== 用户 / 角色 ====================

    @Test
    @DisplayName("用户管理：管理端点 admin，个人端点不标")
    void sysUserController() {
        assertRoles(SysUserController.class, "list", "admin");
        assertRoles(SysUserController.class, "detail", "admin");
        assertRoles(SysUserController.class, "add", "admin");
        assertRoles(SysUserController.class, "update", "admin");
        assertRoles(SysUserController.class, "delete", "admin");
        assertRoles(SysUserController.class, "resetPwd", "admin");
        assertRoles(SysUserController.class, "updateStatus", "admin");
        assertRoles(SysUserController.class, "assignRole", "admin");
        assertNoRoles(SysUserController.class, "login");
        assertNoRoles(SysUserController.class, "captcha");
        assertNoRoles(SysUserController.class, "logout");
        assertNoRoles(SysUserController.class, "updateProfile");
        assertNoRoles(SysUserController.class, "changePwd");
    }

    @Test
    @DisplayName("角色管理：六个端点全部 admin")
    void sysRoleController() {
        assertRoles(SysRoleController.class, "list", "admin");
        assertRoles(SysRoleController.class, "detail", "admin");
        assertRoles(SysRoleController.class, "roleUsers", "admin");
        assertRoles(SysRoleController.class, "add", "admin");
        assertRoles(SysRoleController.class, "update", "admin");
        assertRoles(SysRoleController.class, "delete", "admin");
    }

    // ==================== 断言助手 ====================

    /**
     * 断言方法标注了 @RequiresRoles 且角色值匹配；多角色时必须为 Logical.OR
     */
    private void assertRoles(Class<?> clazz, String methodName, String... expectedRoles) {
        RequiresRoles annotation = findMethod(clazz, methodName).getAnnotation(RequiresRoles.class);
        assertThat(annotation)
                .as(clazz.getSimpleName() + "#" + methodName + " 缺少 @RequiresRoles 注解")
                .isNotNull();
        assertThat(annotation.value())
                .as(clazz.getSimpleName() + "#" + methodName + " 角色值不匹配")
                .containsExactlyInAnyOrder(expectedRoles);
        if (expectedRoles.length > 1) {
            assertThat(annotation.logical())
                    .as(clazz.getSimpleName() + "#" + methodName + " 多角色必须显式 Logical.OR（默认 AND 会收紧到无人可过）")
                    .isEqualTo(Logical.OR);
        }
    }

    /**
     * 断言方法未标注 @RequiresRoles（登录即可或业务自校验）
     */
    private void assertNoRoles(Class<?> clazz, String methodName) {
        assertThat(findMethod(clazz, methodName).getAnnotation(RequiresRoles.class))
                .as(clazz.getSimpleName() + "#" + methodName + " 不应有 @RequiresRoles 注解")
                .isNull();
    }

    private Method findMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("方法不存在：" + clazz.getSimpleName() + "#" + methodName);
    }
}
