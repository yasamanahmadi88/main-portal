export interface NavigationItem {
  readonly path: string;
  readonly labelKey: string;
  readonly icon: string;
  readonly permissions?: readonly string[];
}

export const PRIMARY_NAV: readonly NavigationItem[] = [
  { path: '/dashboard', labelKey: 'navigation.dashboard', icon: 'dashboard' },
  {
    path: '/users',
    labelKey: 'navigation.users',
    icon: 'group',
    permissions: ['USER_READ']
  },
  {
    path: '/roles',
    labelKey: 'navigation.roles',
    icon: 'shield_person',
    permissions: ['ROLE_READ']
  },
  {
    path: '/permissions',
    labelKey: 'navigation.permissions',
    icon: 'lock',
    permissions: ['PERMISSION_READ']
  },
  {
    path: '/audit',
    labelKey: 'navigation.audit',
    icon: 'history',
    permissions: ['AUDIT_READ']
  },
  {
    path: '/security-events',
    labelKey: 'navigation.securityEvents',
    icon: 'security',
    permissions: ['SECURITY_EVENT_READ']
  },
  {
    path: '/settings',
    labelKey: 'navigation.settings',
    icon: 'settings',
    permissions: ['SETTINGS_READ']
  }
];

export const SECONDARY_NAV: readonly NavigationItem[] = [
  { path: '/profile', labelKey: 'navigation.profile', icon: 'person' }
];
