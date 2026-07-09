# Database roles & Flyway migrations

Portal splits its PostgreSQL access between two roles for defence in depth:

| Role | Used by | Privileges |
|------|---------|------------|
| `portal_migration` | Flyway (CI/CD pipeline and one-off DBA sessions) | DDL owner of the `public` schema; may `CREATE`, `ALTER`, `DROP`, `TRUNCATE`. |
| `portal_app`       | Spring Boot runtime                                 | `SELECT / INSERT / UPDATE / DELETE` on all tables, `USAGE, SELECT` on sequences. **No** DDL, **no** `TRUNCATE`, **no** `REFERENCES` on `audit_events`. |

The runtime role must never be able to alter the schema or truncate audit
tables — this is what limits blast radius if the application is compromised.

## Provisioning (production)

```sql
-- Once per database:
CREATE ROLE portal_migration LOGIN PASSWORD :migration_password;
CREATE ROLE portal_app       LOGIN PASSWORD :app_password;

CREATE DATABASE portal OWNER portal_migration;

\connect portal
GRANT CONNECT ON DATABASE portal TO portal_app;
GRANT USAGE ON SCHEMA public TO portal_app;
ALTER SCHEMA public OWNER TO portal_migration;
```

Then run migrations as `portal_migration`:

```bash
DB_URL=jdbc:postgresql://.../portal \
DB_USERNAME=portal_migration \
DB_PASSWORD=... \
./mvnw -pl backend spring-boot:run -Dspring-boot.run.arguments=--spring.flyway.migrate-only=true
```

`V5__db_grants_notes.sql` grants runtime DML to `portal_app` on any tables and
sequences that exist at migration time, and configures default privileges so
future tables inherit the same grants.

## Local development

Docker Compose / Testcontainers spin up a single super-user PostgreSQL, so both
roles are effectively the same DB user. `V5__db_grants_notes.sql` skips its
grants if the runtime role does not exist, keeping local runs frictionless.

## Rotating credentials

1. Provision a second role (e.g. `portal_app_next`) with identical grants.
2. Deploy the application configured to use the new role.
3. Once verified in production, drop the old role.

Never commit passwords — the `.env.example` at the repository root documents
the required environment variables.
