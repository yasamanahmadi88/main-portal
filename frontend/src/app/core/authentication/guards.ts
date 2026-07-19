import { inject } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot
} from '@angular/router';

import { AuthService } from './auth.service';

async function ensureBootstrapped(auth: AuthService): Promise<void> {
  if (auth.status() === 'unknown') {
    await auth.bootstrap();
  }
}

/**
 * Guard that only permits routes when the user has an active session.
 * Redirects to `/auth/login?returnUrl=<original>` otherwise.
 */
export const authGuard: CanActivateFn = async (
  _route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await ensureBootstrapped(auth);
  if (auth.isAuthenticated()) {
    return true;
  }
  if (auth.status() === 'mfa-required') {
    return router.createUrlTree(['/auth/mfa']);
  }
  return router.createUrlTree(['/auth/login'], {
    queryParams: state.url && state.url !== '/' ? { returnUrl: state.url } : {}
  });
};

/**
 * Prevent authenticated users from seeing anonymous-only pages
 * (login, forgot password, …). Sends them to the dashboard.
 */
export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await ensureBootstrapped(auth);
  if (auth.isAuthenticated()) {
    return router.createUrlTree(['/dashboard']);
  }
  return true;
};

/**
 * Guard for routes that require one or more permission codes.
 * Configure via `data: { permissions: ['USER_READ'], mode: 'ANY' }`.
 */
export const permissionGuard: CanActivateFn = async (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await ensureBootstrapped(auth);
  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/auth/login']);
  }
  const raw = route.data?.['permissions'] as string | readonly string[] | undefined;
  if (!raw) {
    return true;
  }
  const codes = Array.isArray(raw) ? [...raw] : [raw as string];
  const mode = (route.data?.['permissionMode'] as 'ANY' | 'ALL' | undefined) ?? 'ANY';
  const ok = mode === 'ALL' ? auth.hasAllPermissions(codes) : auth.hasAnyPermission(codes);
  return ok ? true : router.createUrlTree(['/access-denied']);
};

/**
 * Route-level MFA challenge gate: only render `/auth/mfa` when the auth
 * service is holding a pending challenge (which is populated after login).
 */
export const mfaChallengeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.pendingChallenge()) {
    return true;
  }
  return router.createUrlTree(['/auth/login']);
};
