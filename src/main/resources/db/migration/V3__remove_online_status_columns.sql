-- Remove online status tracking columns since we're using real-time WebSocket tracking
ALTER TABLE users DROP COLUMN IF EXISTS online;
ALTER TABLE users DROP COLUMN IF EXISTS last_seen;