const SAFE_REDIRECT_PATHS = [
  '/dashboard',
  '/profile',
  '/users',
  '/roles',
  '/permissions',
  '/audit',
  '/security-events',
  '/settings'
];

export function isValidRedirectUrl(url: string | null): boolean {
  if (!url) return false;
  if (typeof url !== 'string') return false;
  if (!url.startsWith('/')) return false;
  return SAFE_REDIRECT_PATHS.some(path => url === path || url.startsWith(path + '/'));
}

export function getSafeRedirectUrl(url: string | null, fallback = '/dashboard'): string {
  if (url && isValidRedirectUrl(url)) {
    return url as string;
  }
  return fallback;
}
