#!/bin/sh
# ------------------------------------------------------------------
#  Portal — PostgreSQL role bootstrap (local docker-compose only).
#
#  Executed by the official `postgres` image entrypoint on FIRST
#  startup of a fresh data volume. Provisions:
#
#    * portal_migration — schema owner / Flyway only
#    * portal_app       — runtime DML role (no DDL / no audit truncate)
#
#  Passwords come from environment variables injected by compose
#  (PORTAL_APP_*, PORTAL_MIGRATION_*). They are NEVER hard-coded.
#
#  Implemented as a shell script (not .sql) because psql :'variables'
#  are NOT expanded inside PL/pgSQL DO $$ ... $$ blocks, which caused
#  role creation to fail and Flyway to authenticate as a missing user.
#
#  POSIX sh only — the postgres:alpine image does not ship bash.
# ------------------------------------------------------------------
set -eu

require() {
  eval "val=\${$1:-}"
  if [ -z "$val" ]; then
    echo "ERROR: $1 must be set for database role bootstrap" >&2
    exit 1
  fi
}

require PORTAL_APP_USER
require PORTAL_APP_PASSWORD
require PORTAL_MIGRATION_USER
require PORTAL_MIGRATION_PASSWORD
require POSTGRES_DB
require POSTGRES_USER

# Reject identifiers that cannot be used safely with %I / role names.
for ident in "$PORTAL_APP_USER" "$PORTAL_MIGRATION_USER" "$POSTGRES_DB"; do
  case "$ident" in
    *[!a-zA-Z0-9_]* | [0-9]* | "" )
      echo "ERROR: invalid SQL identifier: ${ident}" >&2
      exit 1
      ;;
  esac
done

echo "Provisioning portal database roles (migration + app)..."

# Use psql -v + format(...)+\\gexec so passwords with special characters
# are passed as bind variables and quoted with %L (never string-interpolated
# into SQL by the shell).
psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname postgres \
  -v mig_user="$PORTAL_MIGRATION_USER" \
  -v mig_password="$PORTAL_MIGRATION_PASSWORD" \
  -v app_user="$PORTAL_APP_USER" \
  -v app_password="$PORTAL_APP_PASSWORD" \
  -v portal_db="$POSTGRES_DB" <<'EOSQL'
SELECT CASE
  WHEN NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'mig_user')
    THEN format('CREATE ROLE %I LOGIN PASSWORD %L', :'mig_user', :'mig_password')
  ELSE format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'mig_user', :'mig_password')
END
\gexec

SELECT CASE
  WHEN NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
    THEN format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_password')
  ELSE format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'app_user', :'app_password')
END
\gexec

SELECT format('ALTER DATABASE %I OWNER TO %I', :'portal_db', :'mig_user')
\gexec
EOSQL

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v mig_user="$PORTAL_MIGRATION_USER" \
  -v app_user="$PORTAL_APP_USER" \
  -v portal_db="$POSTGRES_DB" <<'EOSQL'
SELECT format('ALTER SCHEMA public OWNER TO %I', :'mig_user')
\gexec

SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'portal_db', :'app_user')
\gexec

SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'app_user')
\gexec

SELECT format(
  'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public '
  'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I',
  :'mig_user', :'app_user')
\gexec

SELECT format(
  'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public '
  'GRANT USAGE, SELECT ON SEQUENCES TO %I',
  :'mig_user', :'app_user')
\gexec
EOSQL

echo "Portal database roles provisioned successfully."
