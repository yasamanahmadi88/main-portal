-- V9: Correct audit_event_chain privileges for environments that already
-- applied the earlier V7 (SELECT+INSERT only). The chain cursor must be
-- UPDATE-able so DefaultAuditService can advance last_sequence/last_hash
-- under a pessimistic row lock. audit_events remains append-only.

DO $$
DECLARE
    v_app_role TEXT := '${app_role}';
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_app_role) THEN
        EXECUTE format('GRANT SELECT, INSERT, UPDATE ON TABLE audit_event_chain TO %I', v_app_role);
        EXECUTE format('REVOKE DELETE, TRUNCATE ON TABLE audit_event_chain FROM %I', v_app_role);
        -- Re-assert append-only on the immutable event log.
        EXECUTE format('REVOKE UPDATE, DELETE, TRUNCATE ON TABLE audit_events FROM %I', v_app_role);
        EXECUTE format('GRANT SELECT, INSERT ON TABLE audit_events TO %I', v_app_role);
        RAISE NOTICE 'V9: audit_event_chain UPDATE restored for %', v_app_role;
    ELSE
        RAISE NOTICE 'Runtime role % absent; V9 grants skipped.', v_app_role;
    END IF;
END;
$$;
