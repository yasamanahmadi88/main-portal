import { HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';

import { API_CONFIG } from '@core/configuration/api-config.token';

/**
 * Rewrite short-form URLs starting with `/` to be prefixed with the API
 * base path (e.g. `/api/v1`). Absolute URLs and requests to public assets
 * (e.g. `assets/i18n/*`) pass through unchanged.
 */
export function apiBaseUrlInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  const config = inject(API_CONFIG);
  const url = req.url;
  const passthrough =
    !url.startsWith('/') ||
    url.startsWith(config.baseUrl) ||
    url.startsWith('/assets/') ||
    url.startsWith('/fonts/') ||
    url.startsWith('/api/');
  if (passthrough) {
    return next(req);
  }
  const cloned = req.clone({ url: `${config.baseUrl}${url}` });
  return next(cloned);
}
