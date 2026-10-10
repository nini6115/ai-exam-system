-- =============================================
-- V4: 系统管理模块 - 操作日志表 + 数据字典双表
-- =============================================

SET NAMES utf8mb4;

-- ---------------------------------------------
-- 1. 操作日志表（日志类：只插入不更新，仅 create_time，无 update_time/deleted）
-- ---------------------------------------------
CREATE TABLE `sys_oper_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `module`         VARCHAR(50)  NOT NULL COMMENT '模块名（用户管理/题库管理…）',
    `action`         VARCHAR(50)  NOT NULL COMMENT '操作名（新增用户…）',
    `operator_id`    BIGINT       DEFAULT NULL COMMENT '操作人ID（登录等白名单场景为NULL）',
    `operator_name`  VARCHAR(50)  DEFAULT NULL COMMENT '操作人用户名快照',
    `operator_ip`    VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
    `request_method` VARCHAR(10)  DEFAULT NULL COMMENT '请求方式',
    `request_uri`    VARCHAR(255) DEFAULT NULL COMMENT '请求URI',
    `params`         TEXT         DEFAULT NULL COMMENT '请求参数JSON（密码等敏感字段脱敏，超长截断2000）',
    `result_code`    INT          NOT NULL COMMENT '结果码：200成功 500失败',
    `error_msg`      VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `cost_ms`        BIGINT       DEFAULT NULL COMMENT '耗时毫秒',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_module` (`module`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ---------------------------------------------
-- 2. 字典类型表（配置主体，三件套逻辑删除；uk_dict_code 与逻辑删并存同 uk_username 既有取舍）
-- ---------------------------------------------
CREATE TABLE `sys_dict_type` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
    `dict_name`   VARCHAR(100) NOT NULL COMMENT '字典名称',
    `dict_code`   VARCHAR(50)  NOT NULL COMMENT '字典编码（唯一业务键，关联字典数据）',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态：1启用 0停用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典类型表';

-- ---------------------------------------------
-- 3. 字典数据表（配置项，物理删除无 deleted，避免 @TableLogic 与唯一键冲突）
-- ---------------------------------------------
CREATE TABLE `sys_dict_data` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
    `dict_type_code` VARCHAR(50)  NOT NULL COMMENT '所属字典编码（关联 sys_dict_type.dict_code）',
    `label`          VARCHAR(100) NOT NULL COMMENT '显示名',
    `value`          VARCHAR(100) NOT NULL COMMENT '存储值',
    `sort_order`     INT          DEFAULT 0 COMMENT '排序号',
    `remark`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `status`         TINYINT      DEFAULT 1 COMMENT '状态：1启用 0停用',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_type_value` (`dict_type_code`, `value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典数据表';

-- =============================================
-- 初始字典数据（枚举值与各实体注释一一对应）
-- =============================================

INSERT INTO `sys_dict_type` (`dict_name`, `dict_code`, `description`, `status`) VALUES
('题型',         'question_type',        '题目类型：单选/多选/判断/填空/简答', 1),
('题目难度',     'question_difficulty',  '题目难度等级', 1),
('题目分类',     'question_category',    '题目知识点分类，数据随题库维护', 1),
('考试状态',     'exam_status',          '按考试时间推导的展示状态', 1),
('答卷状态',     'sheet_status',         '答卷生命周期状态', 1),
('判分方式',     'grade_method',         '客观题系统判分/主观题人工与AI判分', 1),
('用户状态',     'user_status',          '账号启用禁用状态', 1),
('性别',         'gender',               '用户性别', 1),
('切屏超限处理', 'screen_switch_action', '防作弊切屏超限后的处理方式', 1);

INSERT INTO `sys_dict_data` (`dict_type_code`, `label`, `value`, `sort_order`) VALUES
('question_type',        '单选题',       '1', 1),
('question_type',        '多选题',       '2', 2),
('question_type',        '判断题',       '3', 3),
('question_type',        '填空题',       '4', 4),
('question_type',        '简答题',       '5', 5),
('question_difficulty',  '简单',         '1', 1),
('question_difficulty',  '中等',         '2', 2),
('question_difficulty',  '困难',         '3', 3),
('exam_status',          '未开始',       '1', 1),
('exam_status',          '进行中',       '2', 2),
('exam_status',          '已结束',       '3', 3),
('sheet_status',         '答题中',       '1', 1),
('sheet_status',         '已交卷',       '2', 2),
('sheet_status',         '强制交卷',     '3', 3),
('sheet_status',         '超时自动交卷', '4', 4),
('grade_method',         '系统判分',     '1', 1),
('grade_method',         '人工判分',     '2', 2),
('grade_method',         'AI判分',       '3', 3),
('user_status',          '正常',         '1', 1),
('user_status',          '禁用',         '0', 2),
('gender',               '未知',         '0', 1),
('gender',               '男',           '1', 2),
('gender',               '女',           '2', 3),
('screen_switch_action', '警告',         '1', 1),
('screen_switch_action', '强制交卷',     '2', 2);

-- 题目分类回填：把题库已有分类灌入字典（分类随题目增删自然演进，此后由管理端维护）
INSERT INTO `sys_dict_data` (`dict_type_code`, `label`, `value`, `sort_order`)
SELECT 'question_category',
       t.`category`,
       t.`category`,
       ROW_NUMBER() OVER (ORDER BY t.`category`)
FROM (SELECT DISTINCT `category`
      FROM `question`
      WHERE `category` IS NOT NULL AND `deleted` = 0) t;
