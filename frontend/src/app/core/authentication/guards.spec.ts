import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { EnvironmentInjector, runInInjectionContext } from '@angular/core';

import { AuthService } from './auth.service';
import { authGuard, guestGuard, mfaChallengeGuard, permissionGuard } from './guards';

function makeSnapshot(url = '/dashboard'): { route: ActivatedRouteSnapshot; state: RouterStateSnapshot } {
  return {
    route: { data: {} } as unknown as ActivatedRouteSnapshot,
    state: { url } as RouterStateSnapshot
  };
}

describe('guards', () => {
  let router: Router;
  let auth: AuthService;
  let injector: EnvironmentInjector;
  let navigateSpy: (commands: unknown[], extras?: unknown) => UrlTree;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    router = TestBed.inject(Router);
    auth = TestBed.inject(AuthService);
    injector = TestBed.inject(EnvironmentInjector);
    navigateSpy = ((commands: unknown[], extras?: unknown) =>
      ({ __url: commands, extras } as unknown as UrlTree));
    (router as unknown as { createUrlTree: typeof navigateSpy }).createUrlTree = navigateSpy;
  });

  it('authGuard allows authenticated users', async () => {
    auth.setUser({
      id: 'u',
      username: 'x',
      email: 'x@x',
      displayName: 'x',
      status: 'ACTIVE',
      preferences: { language: 'fa-IR', theme: 'SYSTEM', timezone: 'Asia/Tehran' },
      roles: [],
      permissions: [],
      mfaEnabled: false,
      createdAt: '',
      updatedAt: ''
    });
    const { route, state } = makeSnapshot();
    const result = await runInInjectionContext(injector, () => authGuard(route, state));
    expect(result).toBe(true);
  });

  it('authGuard redirects to /auth/login with returnUrl', async () => {
    auth.markUnauthenticated('test');
    const { route, state } = makeSnapshot('/users/xyz');
    const result = await runInInjectionContext(injector, () => authGuard(route, state));
    expect((result as any).__url).toEqual(['/auth/login']);
    expect((result as any).extras.queryParams.returnUrl).toBe('/users/xyz');
  });

  it('guestGuard sends authenticated users to dashboard', async () => {
    auth.setUser({
      id: 'u',
      username: 'x',
      email: 'x@x',
      displayName: 'x',
      status: 'ACTIVE',
      preferences: { language: 'fa-IR', theme: 'SYSTEM', timezone: 'Asia/Tehran' },
      roles: [],
      permissions: [],
      mfaEnabled: false,
      createdAt: '',
      updatedAt: ''
    });
    const result = await runInInjectionContext(injector, () =>
      guestGuard(makeSnapshot().route, makeSnapshot().state)
    );
    expect((result as any).__url).toEqual(['/dashboard']);
  });

  it('permissionGuard blocks users lacking required permission', async () => {
    auth.setUser({
      id: 'u',
      username: 'x',
      email: 'x@x',
      displayName: 'x',
      status: 'ACTIVE',
      preferences: { language: 'fa-IR', theme: 'SYSTEM', timezone: 'Asia/Tehran' },
      roles: [],
      permissions: ['USER_READ'],
      mfaEnabled: false,
      createdAt: '',
      updatedAt: ''
    });
    const route = {
      data: { permissions: ['USER_DELETE'] }
    } as unknown as ActivatedRouteSnapshot;
    const result = await runInInjectionContext(injector, () =>
      permissionGuard(route, makeSnapshot().state)
    );
    expect((result as any).__url).toEqual(['/access-denied']);
  });

  it('mfaChallengeGuard blocks when no pending challenge', async () => {
    const result = await runInInjectionContext(injector, () =>
      mfaChallengeGuard(makeSnapshot().route, makeSnapshot().state)
    );
    expect((result as any).__url).toEqual(['/auth/login']);
  });
});
