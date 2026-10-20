-- Migration: Make password nullable for modern auth (Social OAuth / Passkeys)
-- username remains strictly NOT NULL as social and passkey logins require an identifier (email)
ALTER TABLE users
    MODIFY COLUMN password VARCHAR(255) NULL,
    MODIFY COLUMN username VARCHAR(255) NOT NULL;
