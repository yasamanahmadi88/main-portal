import { InjectionToken } from '@angular/core';
import { environment } from '@env/environment';

export interface ApiConfig {
  readonly baseUrl: string;
  readonly csrfCookieName: string;
  readonly csrfHeaderName: string;
  readonly csrfInitEndpoint: string;
  readonly withCredentials: boolean;
}

export const API_CONFIG = new InjectionToken<ApiConfig>('portal.api-config', {
  providedIn: 'root',
  factory: () => ({
    baseUrl: environment.apiBaseUrl,
    csrfCookieName: environment.csrf.cookieName,
    csrfHeaderName: environment.csrf.headerName,
    csrfInitEndpoint: environment.csrf.initEndpoint,
    withCredentials: true
  })
});
