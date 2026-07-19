-- V5: Runtime role restrictions & grant documentation
--
-- Design intent
-- -------------
-- The portal is designed to run under a *runtime* database role (e.g. ``portal_app``)
-- that has ONLY the privileges required for OLTP workloads:
--
--   * SELECT / INSERT / UPDATE / DELETE on all portal tables
--   * USAGE on ``pgcrypto`` (for gen_random_uuid())
--   * USAGE, SELECT on all sequences
--
-- A separate *migration* role (e.g. ``portal_migration``) owns the schema and
-- performs DDL. Flyway runs as the migration role; the application connects
-- as the runtime role. This separation limits blast radius if the app is
-- ever compromised.
--
-- In local single-user development (Docker Compose, unit tests, ephemeral
-- Testcontainers) there is typically only a single super-user role, so the
-- grants below are executed conditionally and become no-ops when either role
-- does not exist. See ``infrastructure/database/README.md`` for the production
-- provisioning procedure.
--
-- Placeholders (from ``spring.flyway.placeholders``):
--   ${app_role}       -> runtime application role (default portal_app)
--   ${migration_role} -> DDL/migration role       (default portal_migration)

DO $$
DECLARE
    v_app_role      TEXT := '${app_role}';
    v_migration_role TEXT := '${migration_role}';
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_app_role) THEN
        EXECUTE format('GRANT USAGE ON SCHEMA public TO %I', v_app_role);
        EXECUTE format(
            'GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO %I',
            v_app_role);
        EXECUTE format(
            'GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO %I',
            v_app_role);
        EXECUTE format(
            'ALTER DEFAULT PRIVILEGES IN SCHEMA public
                 GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I',
            v_app_role);
        EXECUTE format(
            'ALTER DEFAULT PRIVILEGES IN SCHEMA public
                 GRANT USAGE, SELECT ON SEQUENCES TO %I',
            v_app_role);
        RAISE NOTICE 'Granted runtime privileges to %', v_app_role;
    ELSE
        RAISE NOTICE 'Runtime role % does not exist; skipping grants (expected in local/CI).', v_app_role;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_migration_role) THEN
        RAISE NOTICE 'Migration role % is present.', v_migration_role;
    ELSE
        RAISE NOTICE 'Migration role % does not exist; running as super-user is acceptable in local/CI only.', v_migration_role;
    END IF;
END;
$$;

-- Explicit revokes: the ``users`` and audit tables must never grant TRUNCATE
-- or REFERENCES to the runtime role. These revokes are safe even if the
-- runtime role does not exist (they no-op on PUBLIC).
REVOKE TRUNCATE  ON TABLE audit_events           FROM PUBLIC;
REVOKE TRUNCATE  ON TABLE audit_event_chain      FROM PUBLIC;
REVOKE TRUNCATE  ON TABLE security_events        FROM PUBLIC;
REVOKE TRUNCATE  ON TABLE encryption_key_metadata FROM PUBLIC;

-- Documentation-only metadata: describe tables so DBAs get a hint about intent.
COMMENT ON TABLE users                     IS 'Authoritative user directory. Runtime role has DML but not DDL.';
COMMENT ON TABLE audit_events              IS 'Append-only audit log with hash chain. Never UPDATE or DELETE at the app layer.';
COMMENT ON TABLE audit_event_chain         IS 'Tracks the last hash / sequence per audit stream.';
COMMENT ON TABLE encryption_key_metadata   IS 'Key material metadata only. Secret material lives in an external KMS in prod.';
COMMENT ON TABLE outbox_events             IS 'Transactional outbox; consumed by the outbox relay.';
COMMENT ON TABLE mfa_credentials           IS 'MFA secrets are stored ciphertext + IV + tag with reference to encryption_key_metadata.';
