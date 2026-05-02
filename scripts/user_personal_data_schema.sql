-- User personal data persistence for mini-program features.
-- Run this after the existing users and policy_data tables are created.

CREATE TABLE IF NOT EXISTS user_bp_draft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  draft_key VARCHAR(64) NOT NULL,
  project_name VARCHAR(120) NOT NULL,
  one_liner VARCHAR(300),
  form_data JSON,
  content LONGTEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_bp_draft_key (user_id, draft_key),
  KEY idx_user_bp_draft_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_career_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  record_key VARCHAR(64) NOT NULL,
  result_data JSON NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_career_record_key (user_id, record_key),
  KEY idx_user_career_record_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_policy_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  policy_id BIGINT NULL,
  policy_name VARCHAR(255) NOT NULL,
  policy_snapshot JSON,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_policy_favorite (user_id, policy_name),
  KEY idx_user_policy_favorite_user (user_id),
  KEY idx_user_policy_favorite_policy (policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  type VARCHAR(40),
  content VARCHAR(500),
  is_read TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_notification_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_session_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  agent_type VARCHAR(40) NOT NULL,
  input_text LONGTEXT,
  output_text LONGTEXT,
  related_id VARCHAR(64),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_agent_session_user_time (user_id, create_time),
  KEY idx_agent_session_type_time (agent_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
