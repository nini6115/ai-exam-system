-- 考试发布时需要单独设置考试时长（原设计存于 exam_paper.duration，发布后允许覆盖）
ALTER TABLE `exam`
    ADD COLUMN `duration` INT NOT NULL DEFAULT 60 COMMENT '考试时长（分钟）' AFTER `end_time`;
