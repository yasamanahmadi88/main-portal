-- V2: RBAC
-- Roles, permissions, join tables and initial seed data for system roles / permissions.
-- No users are seeded, no passwords in migrations.

CREATE TABLE roles (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    code                        VARCHAR(64)     NOT NULL,
    name                        VARCHAR(128)    NOT NULL,
    description                 VARCHAR(512),
    system_role                 BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT roles_code_unique UNIQUE (code)
);

CREATE TABLE permissions (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    code                        VARCHAR(96)     NOT NULL,
    resource                    VARCHAR(64)     NOT NULL,
    action                      VARCHAR(64)     NOT NULL,
    description                 VARCHAR(512),
    system_permission           BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT permissions_code_unique UNIQUE (code),
    CONSTRAINT permissions_resource_action_unique UNIQUE (resource, action)
);

CREATE TABLE user_roles (
    user_id                     UUID            NOT NULL,
    role_id                     UUID            NOT NULL,
    assigned_at                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    assigned_by                 UUID,
    expires_at                  TIMESTAMPTZ,
    CONSTRAINT user_roles_pk PRIMARY KEY (user_id, role_id),
    CONSTRAINT user_roles_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT user_roles_role_fk FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role ON user_roles (role_id);

CREATE TABLE role_permissions (
    role_id                     UUID            NOT NULL,
    permission_id               UUID            NOT NULL,
    granted_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    granted_by                  UUID,
    CONSTRAINT role_permissions_pk PRIMARY KEY (role_id, permission_id),
    CONSTRAINT role_permissions_role_fk FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT role_permissions_permission_fk FOREIGN KEY (permission_id)
        REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);

-- ---------------------------------------------------------------------------
-- Seed: system permissions
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, resource, action, description, system_permission) VALUES
    ('user:read',          'user',   'read',   'Read user profiles',            TRUE),
    ('user:write',         'user',   'write',  'Modify user profiles',          TRUE),
    ('user:delete',        'user',   'delete', 'Delete or deactivate users',    TRUE),
    ('user:impersonate',   'user',   'impersonate', 'Impersonate other users',  TRUE),
    ('role:read',          'role',   'read',   'Read roles and assignments',    TRUE),
    ('role:write',         'role',   'write',  'Create or modify roles',        TRUE),
    ('permission:read',    'permission', 'read', 'Read permission definitions', TRUE),
    ('permission:write',   'permission', 'write', 'Modify permission grants',   TRUE),
    ('audit:read',         'audit',  'read',   'Read audit events',             TRUE),
    ('audit:export',       'audit',  'export', 'Export audit events',           TRUE),
    ('security:read',      'security','read',  'Read security events',          TRUE),
    ('security:write',     'security','write', 'Manage security policies',      TRUE),
    ('session:read',       'session','read',   'View active sessions',          TRUE),
    ('session:revoke',     'session','revoke', 'Revoke sessions',               TRUE),
    ('settings:read',      'settings','read',  'Read system settings',          TRUE),
    ('settings:write',     'settings','write', 'Modify system settings',        TRUE),
    ('notification:send',  'notification','send','Send notifications',          TRUE),
    ('mfa:manage',         'mfa',    'manage', 'Manage MFA credentials',        TRUE),
    ('self:read',          'self',   'read',   'Read own profile',              TRUE),
    ('self:write',         'self',   'write',  'Modify own profile',            TRUE);

-- ---------------------------------------------------------------------------
-- Seed: system roles
-- ---------------------------------------------------------------------------
INSERT INTO roles (code, name, description, system_role) VALUES
    ('SUPER_ADMIN',   'Super Administrator', 'Unrestricted system administrator', TRUE),
    ('ADMIN',         'Administrator',       'Tenant administrator',              TRUE),
    ('SECURITY_ADMIN','Security Administrator','Security & audit administration', TRUE),
    ('AUDITOR',       'Auditor',             'Read-only audit and security',      TRUE),
    ('USER',          'User',                'Standard authenticated user',       TRUE);

-- Grant every permission to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN';

-- ADMIN gets everything except impersonation
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'user:read','user:write','user:delete',
    'role:read','role:write',
    'permission:read',
    'audit:read','audit:export',
    'security:read',
    'session:read','session:revoke',
    'settings:read','settings:write',
    'notification:send',
    'mfa:manage',
    'self:read','self:write'
)
WHERE r.code = 'ADMIN';

-- SECURITY_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'user:read',
    'role:read',
    'permission:read',
    'audit:read','audit:export',
    'security:read','security:write',
    'session:read','session:revoke',
    'mfa:manage',
    'self:read','self:write'
)
WHERE r.code = 'SECURITY_ADMIN';

-- AUDITOR: read-only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'user:read','role:read','permission:read',
    'audit:read','audit:export',
    'security:read',
    'session:read',
    'settings:read',
    'self:read'
)
WHERE r.code = 'AUDITOR';

-- USER: self-service only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('self:read','self:write')
WHERE r.code = 'USER';
