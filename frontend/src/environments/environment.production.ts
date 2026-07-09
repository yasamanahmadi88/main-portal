import { environment as base } from './environment';

export const environment = {
  ...base,
  production: true
} as const;
