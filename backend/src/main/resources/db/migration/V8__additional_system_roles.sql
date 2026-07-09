-- V8: Additional system roles referenced by the authorization matrix and
-- penetration-test plan (USER_MANAGER, ROLE_MANAGER, SUPPORT).
-- Idempotent: ON CONFLICT DO NOTHING on role code.

INSERT INTO roles (code, name, description, system_role) VALUES
    ('USER_MANAGER', 'User Manager', 'Manage users and assignments within policy', TRUE),
    ('ROLE_MANAGER', 'Role Manager', 'Manage custom roles and permission grants', TRUE),
    ('SUPPORT',      'Support',      'Read-only support access for troubleshooting', TRUE)
ON CONFLICT (code) DO NOTHING;

-- USER_MANAGER: user lifecycle without role/permission structural writes
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'user:read', 'user:write', 'user:delete',
    'role:read',
    'session:read', 'session:revoke',
    'self:read', 'self:write'
)
WHERE r.code = 'USER_MANAGER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ROLE_MANAGER: role/permission administration without user delete / audit export
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'user:read',
    'role:read', 'role:write',
    'permission:read', 'permission:write',
    'self:read', 'self:write'
)
WHERE r.code = 'ROLE_MANAGER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- SUPPORT: read-mostly troubleshooting
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'user:read', 'role:read', 'permission:read',
    'session:read',
    'security:read',
    'settings:read',
    'self:read', 'self:write'
)
WHERE r.code = 'SUPPORT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
