import {
  HttpClient,
  HttpContext,
  HttpErrorResponse,
  HttpParams
} from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { CsrfService } from './csrf.service';
import { SKIP_ERROR_HANDLING } from '@core/error-handling/error.interceptor';
import { LoggerService } from '@core/observability/logger.service';
import type {
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  MfaChallenge,
  MfaRecoveryRequest,
  MfaVerifyRequest,
  ResetPasswordRequest,
  User
} from '@shared/models';

export type AuthStatus =
  | 'unknown'
  | 'authenticating'
  | 'mfa-required'
  | 'authenticated'
  | 'unauthenticated';

const noErrorContext = () => new HttpContext().set(SKIP_ERROR_HANDLING, true);

/**
 * Central authentication state store.
 *
 * Design notes:
 * - No tokens are persisted; the browser session lives in an httpOnly
 *   cookie managed by Spring Session. The frontend only tracks in-memory
 *   status and the currently signed-in user.
 * - A pending {@link MfaChallenge} is kept in-memory so that a page
 *   navigation to `/auth/mfa` does not lose the challenge id.
 * - Signals form the reactive surface so components can render without
 *   RxJS boilerplate.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly csrf = inject(CsrfService);
  private readonly logger = inject(LoggerService);
  private readonly _destroyRef = inject(DestroyRef);

  private readonly _status = signal<AuthStatus>('unknown');
  private readonly _user = signal<User | null>(null);
  private readonly _pendingChallenge = signal<MfaChallenge | null>(null);
  private readonly _lastError = signal<string | null>(null);

  readonly status = this._status.asReadonly();
  readonly user = this._user.asReadonly();
  readonly pendingChallenge = this._pendingChallenge.asReadonly();
  readonly lastError = this._lastError.asReadonly();

  readonly isAuthenticated = computed(() => this._status() === 'authenticated');
  readonly isAnonymous = computed(() =>
    ['unauthenticated', 'mfa-required'].includes(this._status())
  );
  readonly displayName = computed(() => this._user()?.displayName ?? '');
  readonly permissions = computed(() => new Set(this._user()?.permissions ?? []));
  readonly roles = computed(() => new Set(this._user()?.roles.map((r) => r.code) ?? []));

  /**
   * Determines the current authentication state by asking the backend for
   * the current user. Uses `SKIP_ERROR_HANDLING` because a 401 here is
   * expected for anonymous visitors.
   */
  async bootstrap(): Promise<void> {
    try {
      await this.csrf.ensure();
      const user = await firstValueFrom(
        this.http.get<User>('/api/v1/me', {
          withCredentials: true,
          context: noErrorContext()
        })
      );
      this._user.set(user);
      this._status.set('authenticated');
    } catch (err) {
      if (err instanceof HttpErrorResponse && (err.status === 401 || err.status === 0)) {
        this._status.set('unauthenticated');
        this._user.set(null);
      } else {
        this.logger.warn('auth.bootstrap.failed', { message: (err as Error).message });
        this._status.set('unauthenticated');
      }
    }
  }

  async login(payload: LoginRequest): Promise<LoginResponse> {
    this._status.set('authenticating');
    this._lastError.set(null);
    try {
      const response = await firstValueFrom(
        this.http.post<LoginResponse>('/api/v1/auth/login', payload, {
          withCredentials: true,
          context: noErrorContext()
        })
      );
      await this.csrf.refresh();
      if (response.status === 'AUTHENTICATED' && response.user) {
        this._user.set(response.user);
        this._pendingChallenge.set(null);
        this._status.set('authenticated');
      } else if (response.status === 'MFA_REQUIRED' && response.mfaChallenge) {
        this._pendingChallenge.set(response.mfaChallenge);
        this._status.set('mfa-required');
      }
      return response;
    } catch (err) {
      this._status.set('unauthenticated');
      this.captureError(err);
      throw err;
    }
  }

  async verifyMfa(payload: MfaVerifyRequest): Promise<LoginResponse> {
    return this.completeMfa('/api/v1/auth/mfa/verify', payload);
  }

  async submitRecoveryCode(payload: MfaRecoveryRequest): Promise<LoginResponse> {
    return this.completeMfa('/api/v1/auth/mfa/recovery', payload);
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post<void>(
          '/api/v1/auth/logout',
          {},
          { withCredentials: true, context: noErrorContext() }
        )
      );
    } catch (err) {
      this.logger.info('auth.logout.softFail', { message: (err as Error).message });
    } finally {
      this._user.set(null);
      this._pendingChallenge.set(null);
      this._status.set('unauthenticated');
      await this.csrf.refresh();
    }
  }

  async forgotPassword(payload: ForgotPasswordRequest): Promise<void> {
    await this.csrf.ensure();
    await firstValueFrom(
      this.http.post<void>('/api/v1/auth/password/forgot', payload, {
        withCredentials: true
      })
    );
  }

  async resetPassword(payload: ResetPasswordRequest): Promise<void> {
    await this.csrf.ensure();
    await firstValueFrom(
      this.http.post<void>('/api/v1/auth/password/reset', payload, {
        withCredentials: true
      })
    );
  }

  /** Public helper for the interceptor when we hit a 401 mid-session. */
  markUnauthenticated(reason: string): void {
    this.logger.info('auth.marked-unauthenticated', { reason });
    this._user.set(null);
    this._pendingChallenge.set(null);
    this._status.set('unauthenticated');
  }

  /** Update in-memory user (used by profile edits and preferences). */
  setUser(user: User): void {
    this._user.set(user);
    this._status.set('authenticated');
  }

  clearChallenge(): void {
    this._pendingChallenge.set(null);
  }

  hasPermission(code: string): boolean {
    return this.permissions().has(code);
  }

  hasAnyPermission(codes: readonly string[]): boolean {
    const owned = this.permissions();
    return codes.some((c) => owned.has(c));
  }

  hasAllPermissions(codes: readonly string[]): boolean {
    const owned = this.permissions();
    return codes.every((c) => owned.has(c));
  }

  hasRole(code: string): boolean {
    return this.roles().has(code);
  }

  private async completeMfa(
    endpoint: string,
    payload: MfaVerifyRequest | MfaRecoveryRequest
  ): Promise<LoginResponse> {
    try {
      const response = await firstValueFrom(
        this.http.post<LoginResponse>(endpoint, payload, {
          withCredentials: true,
          context: noErrorContext()
        })
      );
      await this.csrf.refresh();
      if (response.status === 'AUTHENTICATED' && response.user) {
        this._user.set(response.user);
        this._pendingChallenge.set(null);
        this._status.set('authenticated');
      }
      return response;
    } catch (err) {
      this.captureError(err);
      throw err;
    }
  }

  private captureError(err: unknown): void {
    if (err instanceof HttpErrorResponse) {
      const problem = err.error as { code?: string; detail?: string; title?: string } | null;
      this._lastError.set(problem?.code ?? problem?.detail ?? problem?.title ?? err.message);
    } else if (err instanceof Error) {
      this._lastError.set(err.message);
    }
  }
}

/** Standalone helper so guards can build `queryParams` uniformly. */
export function buildReturnUrlParams(url: string | undefined): HttpParams | undefined {
  return url ? new HttpParams().set('returnUrl', url) : undefined;
}
