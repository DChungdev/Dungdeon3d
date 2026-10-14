-- Dungeon Delvers Online — Init database (Windows native PostgreSQL)
-- Chạy với superuser postgres:
--   psql -U postgres -f init-postgres.sql

CREATE USER ddo WITH PASSWORD 'ddo';

SELECT 'CREATE DATABASE dungeon_delvers OWNER ddo'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'dungeon_delvers')\gexec

GRANT ALL PRIVILEGES ON DATABASE dungeon_delvers TO ddo;

\connect dungeon_delvers

GRANT ALL ON SCHEMA public TO ddo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ddo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ddo;

\echo 'Database dungeon_delvers ready. User: ddo / Password: ddo'
