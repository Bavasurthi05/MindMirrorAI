-- NOTE: The application schema is now managed by Flyway migrations located at
--   backend/src/main/resources/db/migration/
-- This file only ensures the database exists for local/manual setups.
-- Flyway creates and versions all tables on backend startup.
CREATE DATABASE IF NOT EXISTS mental_health_db;
