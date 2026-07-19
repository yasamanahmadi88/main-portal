import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  ErrorHandler,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection
} from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';

import { PortalErrorHandler } from '@core/error-handling/global-error.handler';
import { acceptLanguageInterceptor } from '@core/http/accept-language.interceptor';
import { apiBaseUrlInterceptor } from '@core/http/api-base.url';
import { csrfInterceptor } from '@core/http/csrf.interceptor';
import { errorInterceptor } from '@core/error-handling/error.interceptor';
import { provideI18n } from '@core/i18n/i18n.providers';
import { provideAppInitialization } from '@core/session/bootstrap.provider';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' })
    ),
    provideAnimationsAsync(),
    provideHttpClient(
      withInterceptors([
        apiBaseUrlInterceptor,
        acceptLanguageInterceptor,
        csrfInterceptor,
        errorInterceptor
      ])
    ),
    provideI18n(),
    provideAppInitialization(),
    { provide: ErrorHandler, useClass: PortalErrorHandler }
  ]
};
