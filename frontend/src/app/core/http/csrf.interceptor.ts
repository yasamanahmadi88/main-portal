import { HttpErrorResponse, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, from, switchMap, throwError } from 'rxjs';

import { CsrfService, SKIP_CSRF } from '@core/authentication/csrf.service';

const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export function csrfInterceptor(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<import('@angular/common/http').HttpEvent<unknown>> {
  const csrf = inject(CsrfService);

  if (req.context.get(SKIP_CSRF) || !UNSAFE_METHODS.has(req.method.toUpperCase())) {
    return next(withCredentials(req));
  }

  return from(csrf.ensure()).pipe(
    switchMap((token) => next(applyCsrf(req, token, csrf.headerName))),
    catchError((err: unknown) => {
      // 403 from Spring's CsrfFilter typically comes back with an
      // "Invalid CSRF token" body. Refresh once and retry the request.
      if (
        err instanceof HttpErrorResponse &&
        err.status === 403 &&
        !req.headers.has('X-Portal-Csrf-Retry')
      ) {
        return from(csrf.refresh()).pipe(
          switchMap((token) => {
            const retry = applyCsrf(req, token, csrf.headerName).clone({
              setHeaders: { 'X-Portal-Csrf-Retry': '1' }
            });
            return next(retry);
          })
        );
      }
      return throwError(() => err);
    })
  );
}

function applyCsrf(
  req: HttpRequest<unknown>,
  token: string | null,
  headerName: string
): HttpRequest<unknown> {
  const withCreds = withCredentials(req);
  if (!token) {
    return withCreds;
  }
  return withCreds.clone({ setHeaders: { [headerName]: token } });
}

function withCredentials(req: HttpRequest<unknown>): HttpRequest<unknown> {
  return req.withCredentials ? req : req.clone({ withCredentials: true });
}
