-- Migration: V3 Add Login History Table and Indexes
-- Target Database: MySQL 8/9 & H2 (inchef_db)

CREATE TABLE IF NOT EXISTS login_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    authentication_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    CONSTRAINT fk_login_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_login_history_user_login_at ON login_history(user_id, login_at DESC);

