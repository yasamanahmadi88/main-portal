import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HttpContext,
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import { CsrfService, SKIP_CSRF } from './csrf.service';
import { csrfInterceptor } from '@core/http/csrf.interceptor';

const clearCookie = () => {
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/';
};

const flush = async () => {
  for (let i = 0; i < 5; i++) {
    await Promise.resolve();
  }
};

describe('CsrfService', () => {
  let service: CsrfService;
  let controller: HttpTestingController;

  beforeEach(() => {
    clearCookie();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([csrfInterceptor])),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CsrfService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
    clearCookie();
  });

  it('fetches a token via /auth/csrf when no cookie is present', async () => {
    const promise = service.ensure(true);
    await flush();
    const req = controller.expectOne('/api/v1/auth/csrf');
    expect(req.request.method).toBe('GET');
    req.flush({ headerName: 'X-XSRF-TOKEN', token: 'abc123' });
    const token = await promise;
    expect(token).toBe('abc123');
    expect(service.token()).toBe('abc123');
  });

  it('reads token from cookie when available', () => {
    document.cookie = 'XSRF-TOKEN=cookie-value; path=/';
    expect(service.readTokenFromCookie()).toBe('cookie-value');
  });

  it('short-circuits ensure() when cookie already carries a value', async () => {
    document.cookie = 'XSRF-TOKEN=from-cookie; path=/';
    const token = await service.ensure();
    expect(token).toBe('from-cookie');
    expect(service.token()).toBe('from-cookie');
  });
});

describe('csrfInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    clearCookie();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([csrfInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
    clearCookie();
  });

  it('does not attach the header on GET requests', async () => {
    http.get('/api/v1/dashboard/summary').subscribe();
    await flush();
    const req = controller.expectOne('/api/v1/dashboard/summary');
    expect(req.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    req.flush({});
  });

  it('attaches X-XSRF-TOKEN on POST when a token is already cached', async () => {
    const csrf = TestBed.inject(CsrfService);
    document.cookie = 'XSRF-TOKEN=unsafe-token; path=/';
    await csrf.ensure();

    http.post('/api/v1/users', {}).subscribe();
    await flush();

    const req = controller.expectOne('/api/v1/users');
    expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('unsafe-token');
    req.flush({});
  });

  it('honors SKIP_CSRF: no header attached even for POST', async () => {
    const context = new HttpContext().set(SKIP_CSRF, true);
    http.post('/api/v1/auth/csrf', {}, { context }).subscribe();
    await flush();
    const req = controller.expectOne('/api/v1/auth/csrf');
    expect(req.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    req.flush({});
  });
});
