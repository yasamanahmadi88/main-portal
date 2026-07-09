import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import {
  TranslateLoader,
  provideTranslateService
} from '@ngx-translate/core';

import { I18nService } from './i18n.service';

class StubLoader implements TranslateLoader {
  getTranslation(lang: string) {
    return of({ greeting: `hello:${lang}` });
  }
}

describe('I18nService', () => {
  beforeEach(() => {
    localStorage.removeItem('portal.lang');
    document.documentElement.setAttribute('lang', 'en-US');
    document.documentElement.setAttribute('dir', 'ltr');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService({
          fallbackLang: 'en-US',
          lang: 'fa-IR',
          loader: { provide: TranslateLoader, useClass: StubLoader }
        })
      ]
    });
  });

  it('honors the stored fa-IR preference and applies RTL', async () => {
    localStorage.setItem('portal.lang', 'fa-IR');
    const service = TestBed.inject(I18nService);
    await service.initialize();

    expect(service.currentLanguage()).toBe('fa-IR');
    expect(service.direction()).toBe('rtl');
    expect(service.isRtl()).toBe(true);
    expect(document.documentElement.getAttribute('lang')).toBe('fa-IR');
    expect(document.documentElement.getAttribute('dir')).toBe('rtl');
  });

  it('switches to en-US and updates direction to ltr', async () => {
    localStorage.setItem('portal.lang', 'fa-IR');
    const service = TestBed.inject(I18nService);
    await service.initialize();

    await service.use('en-US');

    expect(service.currentLanguage()).toBe('en-US');
    expect(service.direction()).toBe('ltr');
    expect(document.documentElement.getAttribute('lang')).toBe('en-US');
    expect(document.documentElement.getAttribute('dir')).toBe('ltr');
    expect(localStorage.getItem('portal.lang')).toBe('en-US');
  });

  it('keeps state consistent when switching back to fa-IR', async () => {
    localStorage.setItem('portal.lang', 'fa-IR');
    const service = TestBed.inject(I18nService);
    await service.initialize();
    await service.use('en-US');
    await service.use('fa-IR');

    expect(service.currentLanguage()).toBe('fa-IR');
    expect(service.direction()).toBe('rtl');
    expect(service.isRtl()).toBe(true);
  });
});
