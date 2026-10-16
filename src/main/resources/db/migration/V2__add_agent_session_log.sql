-- Agent 调用日志表
--
-- 这张表在历史 SQL 里一直没有 DDL，代码里靠 try/catch 兜住「表可能不存在」，
-- 结果 Agent 日志其实一条也写不进去（含 MQ 消费端的落库）。这里补建，
-- 并给 listRecentLogs 的「按用户 + 时间倒序取最近 N 条」加上联合索引。
CREATE TABLE IF NOT EXISTS `agent_session_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID，未登录时为空',
  `agent_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'Agent 类型：policy/career/bp/startup/router',
  `input_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '输入内容',
  `output_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '输出内容',
  `related_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联业务ID（政策ID、会话ID等）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_agent_session_log_user_time` (`user_id`, `create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent 调用日志' ROW_FORMAT = DYNAMIC;
