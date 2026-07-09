-- V7: Enforce append-only privileges on audit tables for the runtime role.
-- V5 granted DML on ALL tables; this migration narrows audit_events /
-- audit_event_chain to SELECT + INSERT only for portal_app.

DO $$
DECLARE
    v_app_role TEXT := '${app_role}';
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_app_role) THEN
        EXECUTE format('REVOKE UPDATE, DELETE, TRUNCATE ON TABLE audit_events FROM %I', v_app_role);
        EXECUTE format('REVOKE UPDATE, DELETE, TRUNCATE ON TABLE audit_event_chain FROM %I', v_app_role);
        EXECUTE format('GRANT SELECT, INSERT ON TABLE audit_events TO %I', v_app_role);
        EXECUTE format('GRANT SELECT, INSERT ON TABLE audit_event_chain TO %I', v_app_role);
        RAISE NOTICE 'Append-only grants applied on audit tables for %', v_app_role;
    ELSE
        RAISE NOTICE 'Runtime role % absent; append-only grants skipped.', v_app_role;
    END IF;
END;
$$;

COMMENT ON TABLE audit_events IS
  'Append-only audit log with hash chain. Runtime role: SELECT+INSERT only; never UPDATE/DELETE.';
