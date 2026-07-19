export const I18N_NAMESPACES = [
  'common',
  'validation',
  'navigation',
  'authentication',
  'dashboard',
  'profile',
  'users',
  'roles',
  'permissions',
  'audit',
  'security-events',
  'settings',
  'monitoring'
] as const;

export type I18nNamespace = (typeof I18N_NAMESPACES)[number];
