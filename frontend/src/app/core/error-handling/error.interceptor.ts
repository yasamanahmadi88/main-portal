import {
  HttpContextToken,
  HttpErrorResponse,
  HttpHandlerFn,
  HttpRequest
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '@core/authentication/auth.service';
import { ToastService } from '@core/observability/toast.service';
import { LoggerService } from '@core/observability/logger.service';
import { normalizeHttpError } from './problem-details';

/** Skip global side effects (toast/logout) for a specific request. */
export const SKIP_ERROR_HANDLING = new HttpContextToken<boolean>(() => false);

export function errorInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  const router = inject(Router);
  const auth = inject(AuthService);
  const toast = inject(ToastService);
  const logger = inject(LoggerService);
  const skip = req.context.get(SKIP_ERROR_HANDLING);

  return next(req).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse)) {
        return throwError(() => err);
      }
      const normalized = normalizeHttpError(err);
      logger.warn('http.error', {
        method: req.method,
        url: req.url,
        status: normalized.status,
        code: normalized.code,
        traceId: normalized.traceId
      });

      if (skip) {
        return throwError(() => err);
      }

      switch (normalized.status) {
        case 401: {
          // Silent for the CSRF fetch and public auth endpoints; otherwise
          // force the app back to the login screen.
          const isAuthEndpoint = req.url.includes('/auth/');
          if (!isAuthEndpoint) {
            auth.markUnauthenticated('http-401');
            const returnUrl = router.url && router.url !== '/' ? router.url : undefined;
            router.navigate(['/auth/session-expired'], {
              queryParams: returnUrl ? { returnUrl } : undefined
            });
          }
          break;
        }
        case 403:
          if (!req.url.includes('/auth/')) {
            toast.error('common.errors.forbidden');
          }
          break;
        case 409:
          toast.warning(normalized.message || 'common.errors.conflict');
          break;
        case 422:
          // Field errors are handled inline; only surface a global toast when
          // there was nothing usable.
          if (Object.keys(normalized.fieldErrors).length === 0) {
            toast.warning(normalized.message || 'validation.errors.generic');
          }
          break;
        case 429:
          toast.warning('common.errors.rateLimited');
          break;
        case 0:
          toast.error('common.errors.network');
          break;
        default:
          if (normalized.status >= 500) {
            toast.error('common.errors.server');
          }
      }

      return throwError(() => err);
    })
  );
}
