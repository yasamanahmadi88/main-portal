import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { CaptchaChallengeComponent } from './captcha-challenge.component';
import type { CaptchaChallenge } from '@shared/models';

const sampleChallenge: CaptchaChallenge = {
  captchaId: 'id-1',
  imageSvg: '<svg xmlns="http://www.w3.org/2000/svg"><text>AB12</text></svg>',
  expiresAt: '2099-01-01T00:00:00Z',
  ttlSeconds: 120,
  revealAnswer: 'AB12'
};

const flushMicrotasks = async () => {
  for (let i = 0; i < 5; i++) {
    await Promise.resolve();
  }
};

describe('CaptchaChallengeComponent', () => {
  let controller: HttpTestingController;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [CaptchaChallengeComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService({ fallbackLang: 'en-US' })
      ]
    }).compileComponents();
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
  });

  it('loads a captcha challenge on init', async () => {
    const fixture = TestBed.createComponent(CaptchaChallengeComponent);
    fixture.detectChanges();
    await flushMicrotasks();

    const req = controller.expectOne('/api/v1/auth/captcha');
    expect(req.request.method).toBe('GET');
    req.flush(sampleChallenge);
    await flushMicrotasks();
    fixture.detectChanges();

    expect(fixture.componentInstance.ready()).toBe(true);
    expect(fixture.componentInstance.captchaId()).toBe('id-1');
    expect(fixture.componentInstance.revealAnswer()).toBe('AB12');
    expect(fixture.nativeElement.querySelector('[data-testid="captcha-image"]')).toBeTruthy();
    expect(
      fixture.nativeElement.querySelector('[data-testid="captcha-reveal"]')?.getAttribute('data-answer')
    ).toBe('AB12');
  });

  it('refresh clears the answer and requests a new challenge', async () => {
    const fixture = TestBed.createComponent(CaptchaChallengeComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    await flushMicrotasks();
    controller.expectOne('/api/v1/auth/captcha').flush(sampleChallenge);
    await flushMicrotasks();

    component.captchaAnswer.set('OLD');
    const refreshPromise = component.refresh();
    await flushMicrotasks();
    expect(component.captchaAnswer()).toBe('');

    controller.expectOne('/api/v1/auth/captcha').flush({
      ...sampleChallenge,
      captchaId: 'id-2',
      revealAnswer: 'ZZ99'
    });
    await refreshPromise;
    fixture.detectChanges();

    expect(component.captchaId()).toBe('id-2');
    expect(component.revealAnswer()).toBe('ZZ99');
    expect(component.ready()).toBe(true);
  });
});
