import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { AuthService } from './auth.service';
import { csrfInterceptor } from '@core/http/csrf.interceptor';
import type { LoginResponse, User } from '@shared/models';

const sampleUser: User = {
  id: 'u1',
  username: 'jdoe',
  email: 'jdoe@example.com',
  displayName: 'Jane Doe',
  status: 'ACTIVE',
  preferences: { language: 'fa-IR', theme: 'SYSTEM', timezone: 'Asia/Tehran' },
  roles: [
    {
      id: 'r1',
      code: 'ADMIN',
      name: 'Administrator',
      system: true,
      permissions: [],
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z'
    }
  ],
  permissions: ['USER_READ', 'USER_UPDATE'],
  mfaEnabled: false,
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z'
};

const flush = async () => {
  for (let i = 0; i < 5; i++) {
    await Promise.resolve();
  }
};

const clearCookies = () => {
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/';
};

describe('AuthService', () => {
  let service: AuthService;
  let controller: HttpTestingController;

  beforeEach(() => {
    clearCookies();
    // Seed a CSRF cookie so ensure() short-circuits without an HTTP call.
    document.cookie = 'XSRF-TOKEN=test-csrf; path=/';
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([csrfInterceptor])),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuthService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
    clearCookies();
  });

  it('initializes to unknown status', () => {
    expect(service.status()).toBe('unknown');
    expect(service.user()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.isAnonymous()).toBe(false);
  });

  it('bootstraps to authenticated when /me responds', async () => {
    const promise = service.bootstrap();
    await flush();
    controller.expectOne('/api/v1/me').flush(sampleUser);
    await promise;
    expect(service.status()).toBe('authenticated');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.hasPermission('USER_READ')).toBe(true);
    expect(service.hasPermission('USER_DELETE')).toBe(false);
    expect(service.hasRole('ADMIN')).toBe(true);
  });

  it('bootstraps to unauthenticated on 401', async () => {
    const promise = service.bootstrap();
    await flush();
    controller
      .expectOne('/api/v1/me')
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    await promise;
    expect(service.status()).toBe('unauthenticated');
    expect(service.isAnonymous()).toBe(true);
  });

  it('completes login and stores user', async () => {
    const promise = service.login({
      username: 'x',
      password: 'y',
      captchaId: 'captcha-1',
      captchaAnswer: 'AB12'
    });
    await flush();
    const loginResponse: LoginResponse = { status: 'AUTHENTICATED', user: sampleUser };
    controller.expectOne('/api/v1/auth/login').flush(loginResponse);
    await flush();
    // AuthService refreshes the CSRF token after login.
    controller
      .expectOne('/api/v1/auth/csrf')
      .flush({ headerName: 'X-XSRF-TOKEN', token: 'rotated' });
    const response = await promise;
    expect(response.status).toBe('AUTHENTICATED');
    expect(service.status()).toBe('authenticated');
    expect(service.user()?.id).toBe('u1');
  });

  it('holds MFA challenge when required', async () => {
    const promise = service.login({
      username: 'x',
      password: 'y',
      captchaId: 'captcha-1',
      captchaAnswer: 'AB12'
    });
    await flush();
    controller.expectOne('/api/v1/auth/login').flush({
      status: 'MFA_REQUIRED',
      mfaChallenge: {
        challengeId: 'c1',
        methods: ['TOTP'],
        expiresAt: '2999-01-01T00:00:00Z'
      }
    });
    await flush();
    controller
      .expectOne('/api/v1/auth/csrf')
      .flush({ headerName: 'X-XSRF-TOKEN', token: 'v2' });
    const response = await promise;
    expect(response.status).toBe('MFA_REQUIRED');
    expect(service.status()).toBe('mfa-required');
    expect(service.pendingChallenge()?.challengeId).toBe('c1');
  });

  it('clears state on logout', async () => {
    service.setUser(sampleUser);
    expect(service.isAuthenticated()).toBe(true);
    const promise = service.logout();
    await flush();
    controller.expectOne('/api/v1/auth/logout').flush(null);
    await flush();
    controller
      .expectOne('/api/v1/auth/csrf')
      .flush({ headerName: 'X-XSRF-TOKEN', token: 'anon' });
    await promise;
    expect(service.status()).toBe('unauthenticated');
    expect(service.user()).toBeNull();
  });

  it('permission helpers respect ALL and ANY semantics', () => {
    service.setUser(sampleUser);
    expect(service.hasAnyPermission(['NOPE', 'USER_READ'])).toBe(true);
    expect(service.hasAllPermissions(['USER_READ', 'USER_UPDATE'])).toBe(true);
    expect(service.hasAllPermissions(['USER_READ', 'USER_DELETE'])).toBe(false);
  });
});
