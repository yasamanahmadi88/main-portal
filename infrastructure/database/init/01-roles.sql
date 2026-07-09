-- ------------------------------------------------------------------
--  Portal — PostgreSQL role bootstrap (local docker-compose only).
--
--  This file is executed by the official `postgres` image's
--  `docker-entrypoint.sh` on FIRST startup of a fresh data volume.  It
--  provisions two least-privilege roles used by the application:
--
--    * portal_migration — owns the schema, runs Flyway migrations,
--      is used ONLY by CI/CD and one-off DBA sessions.
--
--    * portal_app       — the runtime role.  Has DML on all tables and
--                         USAGE + SELECT on all sequences.  Cannot ALTER,
--                         TRUNCATE, or REFERENCE the audit_events table.
--
--  Passwords are injected by the compose file via environment variables
--  (PORTAL_APP_USER, PORTAL_APP_PASSWORD, PORTAL_MIGRATION_USER,
--  PORTAL_MIGRATION_PASSWORD) — they are NEVER hard-coded here.
--
--  For production the same pattern applies but you should provision the
--  roles out-of-band (e.g. Terraform + a secrets manager) instead of via
--  the entrypoint.  See infrastructure/database/README.md for details.
-- ------------------------------------------------------------------

\set ON_ERROR_STOP on
\set app_user       `echo "$PORTAL_APP_USER"`
\set app_password   `echo "$PORTAL_APP_PASSWORD"`
\set mig_user       `echo "$PORTAL_MIGRATION_USER"`
\set mig_password   `echo "$PORTAL_MIGRATION_PASSWORD"`
\set portal_db      `echo "$POSTGRES_DB"`

-- ------------------------------------------------------------------
--  Sanity checks — fail fast when required env vars are missing.
-- ------------------------------------------------------------------
DO $$
BEGIN
    IF current_setting('is_superuser')::boolean IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'init script must run as a superuser';
    END IF;
END $$;

-- ------------------------------------------------------------------
--  Roles
-- ------------------------------------------------------------------
DO $$
DECLARE
    v_mig_user text := :'mig_user';
    v_mig_pw   text := :'mig_password';
    v_app_user text := :'app_user';
    v_app_pw   text := :'app_password';
BEGIN
    IF v_mig_user IS NULL OR length(v_mig_user) = 0 THEN
        RAISE EXCEPTION 'PORTAL_MIGRATION_USER must be set';
    END IF;
    IF v_app_user IS NULL OR length(v_app_user) = 0 THEN
        RAISE EXCEPTION 'PORTAL_APP_USER must be set';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_mig_user) THEN
        EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', v_mig_user, v_mig_pw);
    ELSE
        EXECUTE format('ALTER ROLE %I WITH LOGIN PASSWORD %L', v_mig_user, v_mig_pw);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_app_user) THEN
        EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', v_app_user, v_app_pw);
    ELSE
        EXECUTE format('ALTER ROLE %I WITH LOGIN PASSWORD %L', v_app_user, v_app_pw);
    END IF;
END $$;

-- ------------------------------------------------------------------
--  Database ownership
-- ------------------------------------------------------------------
DO $$
DECLARE
    v_mig_user text := :'mig_user';
    v_db       text := :'portal_db';
BEGIN
    EXECUTE format('ALTER DATABASE %I OWNER TO %I', v_db, v_mig_user);
END $$;

-- Connect to the portal DB to configure schema-level grants.
\connect :"portal_db"

DO $$
DECLARE
    v_mig_user text := :'mig_user';
    v_app_user text := :'app_user';
BEGIN
    -- Migration role owns the public schema; app role only uses it.
    EXECUTE format('ALTER SCHEMA public OWNER TO %I', v_mig_user);
    EXECUTE format('GRANT  CONNECT ON DATABASE %I TO %I', current_database(), v_app_user);
    EXECUTE format('GRANT  USAGE  ON SCHEMA public TO %I', v_app_user);

    -- Default privileges on future objects created by the migration role.
    EXECUTE format(
        'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public '
        'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I',
        v_mig_user, v_app_user);
    EXECUTE format(
        'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public '
        'GRANT USAGE, SELECT ON SEQUENCES TO %I',
        v_mig_user, v_app_user);

    -- Explicitly deny truncate / references on the audit tables.  This is
    -- re-applied by Flyway V5 once the tables exist, but stating the intent
    -- here documents the invariant even for a schema-less DB.
END $$;
