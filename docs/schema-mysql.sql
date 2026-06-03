CREATE DATABASE IF NOT EXISTS careerpilot
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE careerpilot;

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(40) NOT NULL UNIQUE,
  phone VARCHAR(20) UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  salt VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS analysis_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resume_name VARCHAR(255),
  user_id BIGINT,
  match_score INT,
  summary VARCHAR(1000),
  resume_text TEXT,
  job_description TEXT,
  result_json TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_analysis_user_created (user_id, created_at DESC),
  INDEX idx_analysis_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
