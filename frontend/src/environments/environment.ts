export const environment = {
  production: false,
  apiBaseUrl: '/api/v1',
  csrf: {
    cookieName: 'XSRF-TOKEN',
    headerName: 'X-XSRF-TOKEN',
    initEndpoint: '/api/v1/auth/csrf'
  },
  defaultLanguage: 'fa-IR',
  supportedLanguages: ['fa-IR', 'en-US'],
  storageKeys: {
    language: 'portal.lang',
    theme: 'portal.theme'
  },
  session: {
    heartbeatIntervalMs: 60_000,
    idleWarningMinutes: 25
  },
  features: {
    enablePasswordResetLink: true,
    enableRegistration: false
  }
} as const;

export type Environment = typeof environment;
