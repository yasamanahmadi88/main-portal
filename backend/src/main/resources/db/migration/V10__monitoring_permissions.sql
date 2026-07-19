-- V10: Monitoring platform permissions and role grants.
-- Codes use resource:action form; API DTOs expose MONITORING_* uppercase.

INSERT INTO permissions (code, resource, action, description, system_permission) VALUES
    ('monitoring:read',         'monitoring', 'read',         'Access monitoring overview', TRUE),
    ('monitoring:metrics:read', 'monitoring', 'metrics_read', 'Read application metrics summaries', TRUE),
    ('monitoring:logs:read',    'monitoring', 'logs_read',    'Read sanitized operational log summaries', TRUE),
    ('monitoring:traces:read',  'monitoring', 'traces_read',  'Read trace correlation summaries', TRUE)
ON CONFLICT (code) DO NOTHING;

-- SUPER_ADMIN receives every permission (including new ones)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- SECURITY_ADMIN: full monitoring read
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'monitoring:read',
    'monitoring:metrics:read',
    'monitoring:logs:read',
    'monitoring:traces:read'
)
WHERE r.code = 'SECURITY_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ADMIN: overview + metrics
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'monitoring:read',
    'monitoring:metrics:read'
)
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- SUPPORT: overview only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'monitoring:read'
WHERE r.code = 'SUPPORT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
