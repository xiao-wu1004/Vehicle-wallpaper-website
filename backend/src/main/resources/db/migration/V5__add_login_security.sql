ALTER TABLE user_accounts
  ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0;

ALTER TABLE user_accounts
  ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP NULL;
