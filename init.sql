-- Database initialization script
-- This file is automatically executed when PostgreSQL container starts

-- Create the main database (if not exists)
SELECT 'CREATE DATABASE chatapp'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'chatapp')\gexec

-- Create extensions (if needed)
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- You can add any additional initialization scripts here
-- For example: creating specific schemas, users, or initial data

-- Example: Create additional schemas
-- CREATE SCHEMA IF NOT EXISTS chat_schema;
-- CREATE SCHEMA IF NOT EXISTS user_schema;