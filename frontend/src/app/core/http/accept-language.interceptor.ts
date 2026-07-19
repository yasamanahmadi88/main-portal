import { HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';

import { I18nService } from '@core/i18n/i18n.service';

export function acceptLanguageInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  const i18n = inject(I18nService);
  const lang = i18n.currentLanguage();
  if (req.headers.has('Accept-Language')) {
    return next(req);
  }
  const cloned = req.clone({ setHeaders: { 'Accept-Language': lang } });
  return next(cloned);
}
