import {
  APP_INITIALIZER,
  EnvironmentProviders,
  inject,
  makeEnvironmentProviders,
  provideAppInitializer
} from '@angular/core';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateMultiHttpLoader } from '@ngx-translate/http-loader';

import { environment } from '@env/environment';
import { I18N_NAMESPACES } from './i18n.namespaces';
import { I18nService } from './i18n.service';

export function provideI18n(): EnvironmentProviders {
  return makeEnvironmentProviders([
    provideTranslateService({
      fallbackLang: 'en-US',
      lang: environment.defaultLanguage
    }),
    provideTranslateMultiHttpLoader({
      // Absolute paths are required: relative `assets/...` resolves under the
      // current route (e.g. `/auth/login` → `/auth/assets/...`) and never loads,
      // which leaves APP_INITIALIZER pending and the SPA stuck on "Loading…".
      resources: I18N_NAMESPACES.map((ns) => ({
        prefix: `/assets/i18n/${ns}/`,
        suffix: '.json'
      })),
      useHttpBackend: true
    }),
    provideAppInitializer(() => {
      const i18n = inject(I18nService);
      // Bound the wait so a single failed namespace cannot hang bootstrap forever.
      return Promise.race([
        i18n.initialize(),
        new Promise<void>((resolve) => {
          setTimeout(() => resolve(), 8_000);
        })
      ]);
    })
  ]);
}

// APP_INITIALIZER re-export kept so tests can construct without provideAppInitializer.
export { APP_INITIALIZER };
