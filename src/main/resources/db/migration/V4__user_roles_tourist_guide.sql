-- Rename tourist table to users and add role column

-- 1. Drop FK constraints referencing tourist table
ALTER TABLE accounts DROP CONSTRAINT accounts_user_id_fkey;
ALTER TABLE tour DROP CONSTRAINT tour_user_id_fkey;

-- 2. Rename tourist table to users
ALTER TABLE tourist RENAME TO users;

-- 3. Add role column (default TOURIST for existing rows)
ALTER TABLE users ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'TOURIST';

-- 4. Re-add FK constraints pointing to users
ALTER TABLE accounts ADD CONSTRAINT accounts_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id);
ALTER TABLE tour ADD CONSTRAINT tour_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id);

-- 5. Create tourist profile table
CREATE TABLE tourist (
    tourist_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL UNIQUE REFERENCES users(user_id),
    language    VARCHAR(100),
    nationality VARCHAR(100)
);
CREATE INDEX idx_tourist_user_id ON tourist(user_id);

-- 6. Create guide profile table
CREATE TABLE guide (
    guide_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(user_id),
    bio             TEXT,
    specializations VARCHAR(500),
    license_no      VARCHAR(100),
    rating          DOUBLE PRECISION DEFAULT 0.0
);
CREATE INDEX idx_guide_user_id ON guide(user_id);

-- 7. Create tourist profile rows for all existing users
INSERT INTO tourist (user_id)
SELECT user_id FROM users;
