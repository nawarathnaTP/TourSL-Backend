ALTER TABLE accounts DROP COLUMN provider_user_id;
ALTER TABLE accounts ADD COLUMN provider_user_id VARCHAR(255);