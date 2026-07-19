import {
  EnvironmentProviders,
  inject,
  makeEnvironmentProviders,
  provideAppInitializer
} from '@angular/core';

import { CsrfService } from '@core/authentication/csrf.service';
import { AuthService } from '@core/authentication/auth.service';
import { LoggerService } from '@core/observability/logger.service';
import { PreferencesSyncService } from './preferences-sync.service';

export function provideAppInitialization(): EnvironmentProviders {
  return makeEnvironmentProviders([
    provideAppInitializer(async () => {
      const csrf = inject(CsrfService);
      const auth = inject(AuthService);
      const prefs = inject(PreferencesSyncService);
      const logger = inject(LoggerService);
      // Effects must be created in an injection context. Call install() before
      // any await — otherwise Angular throws NG0203 and APP_INITIALIZER fails,
      // leaving the SPA stuck on the static Loading placeholder.
      prefs.install();
      try {
        // Never block SPA mount indefinitely if the API is slow/unreachable.
        await Promise.race([
          (async () => {
            await csrf.ensure();
            await auth.bootstrap();
          })(),
          new Promise<void>((resolve) => {
            setTimeout(() => resolve(), 8_000);
          })
        ]);
      } catch (err) {
        logger.warn('bootstrap.failed', { message: (err as Error).message });
      }
    })
  ]);
}
