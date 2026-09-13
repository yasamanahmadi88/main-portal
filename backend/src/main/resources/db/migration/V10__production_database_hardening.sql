-- V10: Production Database Hardening and Compliance Documentation
--
-- This migration documents production database configuration requirements
-- and enforces critical security constraints for audit and system tables.
--
-- COMPLIANCE REFERENCES:
--   * ASVS V8.4: Partition Sessions / Track Session Validity
--   * ASVS V8.5: Account Access Control
--
-- Design intent
-- =============
-- The portal uses separate database roles for migrations (DDL) and runtime (OLTP).
-- The runtime role ('portal_app') has minimal necessary privileges:
--   * SELECT on all tables (read application data)
--   * INSERT on mutable tables (create new records)
--   * UPDATE/DELETE on mutable tables EXCEPT audit tables (modify user data)
--   * APPEND-ONLY on audit_events and audit_event_chain (compliance requirement)
--
-- Audit tables are append-only: once written, they can never be modified or deleted.
-- This prevents attackers from covering tracks if the application layer is compromised.
--
-- Production Deployment Checklist
-- ================================
-- BEFORE GOING LIVE, verify:
--   1. Separate roles exist:
--      CREATE ROLE portal_migration WITH LOGIN PASSWORD '...';
--      CREATE ROLE portal_app WITH LOGIN PASSWORD '...';
--   2. schema is owned by portal_migration
--   3. portal_migration role can connect to production database
--   4. Flyway is configured to run as portal_migration
--   5. Application connects only as portal_app
--   6. Network access is restricted to application hosts (pg_hba.conf)
--   7. Automated backups are enabled and restore-tested
--   8. Backup encryption key is stored in your KMS and documented
--
-- Backup Strategy
-- ===============
-- Production backups use pg_dump with custom format for point-in-time recovery:
--   pg_dump -h $PG_HOST -U $BACKUP_ROLE -F c $DB_NAME > backup-$(date +%s).dump
--
-- Backup retention: minimum 30 days for audit compliance.
-- Encryption: store dump files encrypted in S3 or similar with KMS key access.
-- Restore testing: monthly dry-run restore to a staging environment.
--
-- The audit_events and audit_event_chain tables are immutable after insertion,
-- so database-level recovery is the ONLY way to remediate corrupted audit data.

DO $$
DECLARE
    v_app_role      TEXT := '${app_role}';
    v_migration_role TEXT := '${migration_role}';
BEGIN
    -- Ensure the runtime role has append-only constraints on critical tables.
    -- These constraints were introduced in V7; this migration documents why.
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_app_role) THEN
        -- Revoke dangerous privileges on audit tables
        EXECUTE format('REVOKE ALL ON TABLE audit_events FROM %I', v_app_role);
        EXECUTE format('REVOKE ALL ON TABLE audit_event_chain FROM %I', v_app_role);

        -- Grant SELECT + INSERT only (append-only)
        EXECUTE format('GRANT SELECT, INSERT ON TABLE audit_events TO %I', v_app_role);
        EXECUTE format('GRANT SELECT, INSERT ON TABLE audit_event_chain TO %I', v_app_role);

        -- Grants on sequences for audit tables
        IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'audit_events_id_seq') THEN
            EXECUTE format('GRANT USAGE, SELECT ON audit_events_id_seq TO %I', v_app_role);
        END IF;

        RAISE NOTICE 'Production audit table constraints enforced for %', v_app_role;
    ELSE
        RAISE NOTICE 'Runtime role % absent; append-only constraints skipped.', v_app_role;
    END IF;
END;
$$;

-- Schema and table documentation for production operations
DO $$
BEGIN
    COMMENT ON SCHEMA public IS
      'Application schema. Owned by portal_migration; runtime role (portal_app) has limited DML.';

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        COMMENT ON TABLE users IS
          'Authoritative user directory (ASVS V8.5: User Account Control). Runtime role: SELECT, INSERT, UPDATE, DELETE (standard CRUD).';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'audit_events') THEN
        COMMENT ON TABLE audit_events IS
          'Append-only audit log (ASVS V8.4: Session Tracking; ASVS V8.5: Privilege Changes). Runtime role: SELECT, INSERT ONLY. Immutable after creation to prevent attack cover-up.';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'audit_event_chain') THEN
        COMMENT ON TABLE audit_event_chain IS
          'Hash chain metadata for append-only verification (ASVS V8.4). Runtime role: SELECT, INSERT ONLY. Links sequential audit events to detect tampering.';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'security_events') THEN
        COMMENT ON TABLE security_events IS
          'High-risk security events (login failures, lockouts, privilege changes). Runtime role: standard DML.';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'mfa_credentials') THEN
        COMMENT ON TABLE mfa_credentials IS
          'MFA secrets encrypted with reference to encryption_key_metadata. Runtime role: standard DML.';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'sessions') THEN
        COMMENT ON TABLE sessions IS
          'User session records for concurrent session limits and timeout enforcement (ASVS V8.4). Runtime role: standard DML.';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'encryption_key_metadata') THEN
        COMMENT ON TABLE encryption_key_metadata IS
          'References to encryption keys; actual key material stored in external KMS. Runtime role: standard DML.';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'outbox_events') THEN
        COMMENT ON TABLE outbox_events IS
          'Transactional outbox pattern for reliable event publishing. Runtime role: standard DML. Consumed by outbox relay.';
    END IF;
END;
$$;
