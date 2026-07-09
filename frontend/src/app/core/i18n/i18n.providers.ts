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
      resources: I18N_NAMESPACES.map((ns) => ({
        prefix: `assets/i18n/${ns}/`,
        suffix: '.json'
      })),
      useHttpBackend: true
    }),
    provideAppInitializer(() => {
      const i18n = inject(I18nService);
      return i18n.initialize();
    })
  ]);
}

// APP_INITIALIZER re-export kept so tests can construct without provideAppInitializer.
export { APP_INITIALIZER };
