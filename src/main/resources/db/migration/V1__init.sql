-- =============================================
-- AI 辅助在线考试系统 - 初始化建表脚本
-- 引擎: InnoDB, 字符集: utf8mb4
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------
-- 1. 用户表
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`        VARCHAR(50)     NOT NULL COMMENT '登录账号',
    `password`        VARCHAR(100)    NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name`       VARCHAR(50)     NOT NULL COMMENT '真实姓名',
    `user_no`         VARCHAR(30)     DEFAULT NULL COMMENT '学号/工号',
    `gender`          TINYINT         DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `phone`           VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    `email`           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    `avatar`          VARCHAR(255)    DEFAULT NULL COMMENT '头像URL',
    `status`          TINYINT         DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `last_login_time` DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(50)     DEFAULT NULL COMMENT '最后登录IP',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT         DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_real_name` (`real_name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ---------------------------------------------
-- 2. 角色表
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code`   VARCHAR(50)     NOT NULL COMMENT '角色编码',
    `role_name`   VARCHAR(50)     NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255)    DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ---------------------------------------------
-- 3. 用户角色关联表
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ---------------------------------------------
-- 4. 题目表（题库）
-- ---------------------------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `type`        TINYINT         NOT NULL COMMENT '题型：1单选 2多选 3判断 4填空 5简答',
    `difficulty`  TINYINT         DEFAULT 2 COMMENT '难度：1简单 2中等 3困难',
    `category`    VARCHAR(100)    DEFAULT NULL COMMENT '知识点/分类',
    `title`       TEXT            NOT NULL COMMENT '题干',
    `options`     JSON            DEFAULT NULL COMMENT '选项（JSON数组）',
    `answer`      TEXT            DEFAULT NULL COMMENT '参考答案',
    `analysis`    TEXT            DEFAULT NULL COMMENT '答案解析',
    `score`       DECIMAL(5,2)    DEFAULT 0.00 COMMENT '默认分值',
    `creator_id`  BIGINT          NOT NULL COMMENT '创建人ID',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_difficulty` (`difficulty`),
    KEY `idx_category` (`category`),
    KEY `idx_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';

-- ---------------------------------------------
-- 5. 试卷表
-- ---------------------------------------------
DROP TABLE IF EXISTS `exam_paper`;
CREATE TABLE `exam_paper` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
    `name`            VARCHAR(200)    NOT NULL COMMENT '试卷名称',
    `description`     VARCHAR(500)    DEFAULT NULL COMMENT '试卷说明',
    `total_score`     DECIMAL(6,2)    NOT NULL COMMENT '总分',
    `pass_score`      DECIMAL(6,2)    NOT NULL COMMENT '及格分',
    `duration`        INT             NOT NULL COMMENT '考试时长（分钟）',
    `question_count`  INT             DEFAULT 0 COMMENT '题目总数',
    `creator_id`      BIGINT          NOT NULL COMMENT '创建人ID',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_creator` (`creator_id`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷表';

-- ---------------------------------------------
-- 6. 试卷-题目关联表
-- ---------------------------------------------
DROP TABLE IF EXISTS `exam_paper_question`;
CREATE TABLE `exam_paper_question` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT,
    `paper_id`        BIGINT          NOT NULL COMMENT '试卷ID',
    `question_id`     BIGINT          NOT NULL COMMENT '题目ID',
    `question_score`  DECIMAL(5,2)    NOT NULL COMMENT '本题分值',
    `sort_order`      INT             NOT NULL COMMENT '排序号',
    `section`         VARCHAR(50)     DEFAULT NULL COMMENT '大题分组',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`),
    KEY `idx_paper_id` (`paper_id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷题目关联表';

-- ---------------------------------------------
-- 7. 考试发布表
-- ---------------------------------------------
DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '考试ID',
    `name`                  VARCHAR(200)    NOT NULL COMMENT '考试名称',
    `paper_id`              BIGINT          NOT NULL COMMENT '试卷ID',
    `creator_id`            BIGINT          NOT NULL COMMENT '发布人ID',
    `start_time`            DATETIME        NOT NULL COMMENT '开始时间',
    `end_time`              DATETIME        NOT NULL COMMENT '截止时间',
    `allow_late_minutes`    INT             DEFAULT 0 COMMENT '允许迟到分钟数',
    `max_attempts`          TINYINT         DEFAULT 1 COMMENT '允许考试次数',
    `random_order`          TINYINT         DEFAULT 1 COMMENT '题目乱序：1是 0否',
    `random_options`        TINYINT         DEFAULT 1 COMMENT '选项乱序：1是 0否',
    `max_screen_switch`     INT             DEFAULT 5 COMMENT '最大切屏次数（0不限）',
    `screen_switch_action`  TINYINT         DEFAULT 1 COMMENT '超限处理：1警告 2强制交卷',
    `away_timeout`          INT             DEFAULT 60 COMMENT '离开超时秒数（0不限）',
    `forbid_copy`           TINYINT         DEFAULT 1 COMMENT '禁止复制：1是 0否',
    `show_score_after`      TINYINT         DEFAULT 0 COMMENT '交卷即显成绩：1是 0否',
    `status`                TINYINT         DEFAULT 1 COMMENT '状态：1未开始 2进行中 3已结束',
    `create_time`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_creator` (`creator_id`),
    KEY `idx_paper_id` (`paper_id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试发布表';

-- ---------------------------------------------
-- 8. 考试-考生关联表
-- ---------------------------------------------
DROP TABLE IF EXISTS `exam_user`;
CREATE TABLE `exam_user` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `exam_id`     BIGINT          NOT NULL COMMENT '考试ID',
    `user_id`     BIGINT          NOT NULL COMMENT '考生ID',
    `attempts`    TINYINT         DEFAULT 0 COMMENT '已考次数',
    `best_score`  DECIMAL(6,2)    DEFAULT NULL COMMENT '最高分',
    `assign_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_user` (`exam_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试考生关联表';

-- ---------------------------------------------
-- 9. 答卷表
-- ---------------------------------------------
DROP TABLE IF EXISTS `answer_sheet`;
CREATE TABLE `answer_sheet` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '答卷ID',
    `exam_id`               BIGINT          NOT NULL COMMENT '考试ID',
    `paper_id`              BIGINT          NOT NULL COMMENT '试卷ID（冗余）',
    `user_id`               BIGINT          NOT NULL COMMENT '考生ID',
    `attempt_no`            TINYINT         DEFAULT 1 COMMENT '第几次',
    `start_time`            DATETIME        NOT NULL COMMENT '开始时间',
    `submit_time`           DATETIME        DEFAULT NULL COMMENT '交卷时间',
    `end_time`              DATETIME        NOT NULL COMMENT '应交卷时间',
    `status`                TINYINT         DEFAULT 1 COMMENT '状态：1答题中 2已交卷 3强制交卷 4超时自动交卷',
    `total_score`           DECIMAL(6,2)    DEFAULT NULL COMMENT '总分',
    `objective_score`       DECIMAL(6,2)    DEFAULT NULL COMMENT '客观题得分',
    `subjective_score`      DECIMAL(6,2)    DEFAULT NULL COMMENT '主观题得分',
    `is_passed`             TINYINT         DEFAULT NULL COMMENT '是否及格',
    `screen_switch_count`   INT             DEFAULT 0 COMMENT '切屏次数',
    `ip_address`            VARCHAR(50)     DEFAULT NULL COMMENT 'IP地址',
    `user_agent`            VARCHAR(500)    DEFAULT NULL COMMENT '浏览器UA',
    `question_order_seed`   INT             NOT NULL COMMENT '题目乱序种子',
    `submit_type`           TINYINT         DEFAULT NULL COMMENT '交卷方式：1手动 2超时 3切屏超限 4管理员强制',
    `create_time`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_exam_user` (`exam_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答卷表';

-- ---------------------------------------------
-- 10. 答题明细表
-- ---------------------------------------------
DROP TABLE IF EXISTS `answer_detail`;
CREATE TABLE `answer_detail` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT,
    `sheet_id`        BIGINT          NOT NULL COMMENT '答卷ID',
    `question_id`     BIGINT          NOT NULL COMMENT '题目ID',
    `user_answer`     TEXT            DEFAULT NULL COMMENT '考生答案',
    `correct_answer`  TEXT            DEFAULT NULL COMMENT '参考答案（冗余）',
    `score`           DECIMAL(5,2)    DEFAULT NULL COMMENT '本题得分',
    `full_score`      DECIMAL(5,2)    NOT NULL COMMENT '本题满分（冗余）',
    `is_correct`      TINYINT         DEFAULT NULL COMMENT '是否正确：1是 0否 NULL(主观题待判)',
    `sort_order`      INT             NOT NULL COMMENT '答卷中的题号',
    `answer_time`     DATETIME        DEFAULT NULL COMMENT '最后作答时间',
    `graded_by`       TINYINT         DEFAULT NULL COMMENT '判分方式：1系统 2人工 3 AI',
    `grader_id`       BIGINT          DEFAULT NULL COMMENT '判分人ID',
    `grade_time`      DATETIME        DEFAULT NULL COMMENT '判分时间',
    `grade_comment`   VARCHAR(500)    DEFAULT NULL COMMENT '判分评语',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sheet_question` (`sheet_id`, `question_id`),
    KEY `idx_sheet_id` (`sheet_id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题明细表';

-- ---------------------------------------------
-- 11. 作弊/异常行为记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS `cheat_record`;
CREATE TABLE `cheat_record` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `sheet_id`      BIGINT          NOT NULL COMMENT '答卷ID',
    `exam_id`       BIGINT          NOT NULL COMMENT '考试ID（冗余）',
    `user_id`       BIGINT          NOT NULL COMMENT '考生ID（冗余）',
    `type`          TINYINT         NOT NULL COMMENT '类型：1切屏 2离开超时 3复制粘贴 4多端登录 5其他',
    `description`   VARCHAR(500)    DEFAULT NULL COMMENT '描述',
    `detail`        JSON            DEFAULT NULL COMMENT '详细信息（JSON）',
    `occur_time`    DATETIME        NOT NULL COMMENT '发生时间',
    `handled`       TINYINT         DEFAULT 0 COMMENT '是否处理：0未 1已',
    `handler_id`    BIGINT          DEFAULT NULL COMMENT '处理人ID',
    `handle_time`   DATETIME        DEFAULT NULL COMMENT '处理时间',
    `handle_note`   VARCHAR(500)    DEFAULT NULL COMMENT '处理备注',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sheet_id` (`sheet_id`),
    KEY `idx_exam_id` (`exam_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_occur_time` (`occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作弊异常记录表';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 初始数据
-- =============================================

-- 角色
INSERT INTO `sys_role` (`role_code`, `role_name`, `description`) VALUES
('admin',   '超级管理员', '系统最高权限'),
('teacher', '教师',       '题库、试卷、考试管理'),
('student', '学生',       '参加考试、查看成绩');

-- 管理员账号：admin / admin123
-- BCrypt 加密值: $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `user_no`, `status`) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', 'ADMIN001', 1);

-- 管理员角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);
