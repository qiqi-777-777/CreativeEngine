-- ============================================================
-- 政策数据表扩展 — 新增来源元数据字段
-- 执行方式：在 Navicat 或 MySQL 客户端中直接执行
-- ============================================================

ALTER TABLE `policy_data`
  ADD COLUMN `source_url`   VARCHAR(500)  NULL COMMENT '原文链接' AFTER `content`,
  ADD COLUMN `source_name`  VARCHAR(200)  NULL COMMENT '来源' AFTER `source_url`,
  ADD COLUMN `publish_date` VARCHAR(50)   NULL COMMENT '发布日期' AFTER `source_name`,
  ADD COLUMN `agency`       VARCHAR(300)  NULL COMMENT '发文机关' AFTER `publish_date`,
  ADD COLUMN `doc_no`       VARCHAR(200)  NULL COMMENT '发文字号' AFTER `agency`;

-- 原文链接唯一索引，用于去重
ALTER TABLE `policy_data` ADD UNIQUE INDEX `idx_source_url` (`source_url`);
