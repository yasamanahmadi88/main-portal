import { HttpClient, HttpContext, HttpContextToken } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { API_CONFIG } from '@core/configuration/api-config.token';
import type { CsrfToken } from '@shared/models';

/** Skip attaching the CSRF header (used by the CSRF fetch itself). */
export const SKIP_CSRF = new HttpContextToken<boolean>(() => false);

/**
 * Manages the CSRF token used for unsafe requests.
 *
 * The backend implements the double-submit cookie pattern: on the first
 * request it issues an `XSRF-TOKEN` cookie and returns the same value in
 * the response body of `GET /auth/csrf`. The frontend must echo it in the
 * `X-XSRF-TOKEN` header for every non-idempotent request.
 *
 * We keep the token in a signal so the interceptor can attach it
 * synchronously and callers (e.g. login, logout) can refresh it after a
 * session boundary — because Spring Session rotates the CSRF value on
 * authentication changes.
 */
@Injectable({ providedIn: 'root' })
export class CsrfService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(API_CONFIG);

  private readonly _token = signal<string | null>(null);
  private inflight: Promise<string | null> | null = null;

  readonly token = this._token.asReadonly();
  readonly headerName = this.config.csrfHeaderName;

  /** Value from the current XSRF-TOKEN cookie, if any. */
  readTokenFromCookie(): string | null {
    if (typeof document === 'undefined') {
      return null;
    }
    const name = `${this.config.csrfCookieName}=`;
    const parts = document.cookie ? document.cookie.split(';') : [];
    for (const raw of parts) {
      const c = raw.trim();
      if (c.startsWith(name)) {
        try {
          return decodeURIComponent(c.substring(name.length));
        } catch {
          return c.substring(name.length);
        }
      }
    }
    return null;
  }

  /**
   * Ensures a CSRF token is available. Reuses an in-flight fetch to prevent
   * duplicate calls when several unsafe requests race on startup.
   */
  async ensure(force = false): Promise<string | null> {
    if (!force && this._token()) {
      return this._token();
    }
    const cookieValue = this.readTokenFromCookie();
    if (!force && cookieValue) {
      this._token.set(cookieValue);
      return cookieValue;
    }
    if (this.inflight) {
      return this.inflight;
    }
    this.inflight = this.fetchToken().finally(() => {
      this.inflight = null;
    });
    return this.inflight;
  }

  /** Invalidate the cached token and re-fetch (call after login/logout). */
  async refresh(): Promise<string | null> {
    this._token.set(null);
    return this.ensure(true);
  }

  private async fetchToken(): Promise<string | null> {
    try {
      const context = new HttpContext().set(SKIP_CSRF, true);
      const response = await firstValueFrom(
        this.http.get<CsrfToken>(this.config.csrfInitEndpoint, {
          withCredentials: true,
          context
        })
      );
      const value = response?.token ?? this.readTokenFromCookie();
      this._token.set(value ?? null);
      return value ?? null;
    } catch {
      // Startup should tolerate a missing backend so the login page still renders.
      const cookieFallback = this.readTokenFromCookie();
      this._token.set(cookieFallback);
      return cookieFallback;
    }
  }
}
