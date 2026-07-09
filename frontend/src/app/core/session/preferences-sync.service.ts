import {
  Injectable,
  Injector,
  effect,
  inject,
  runInInjectionContext,
  untracked
} from '@angular/core';

import { AuthService } from '@core/authentication/auth.service';
import { MeApi } from '@core/http/api/me.api';
import { I18nService } from '@core/i18n/i18n.service';
import { ThemeService } from '@core/i18n/theme.service';
import { LoggerService } from '@core/observability/logger.service';
import type { Language, PreferencesPatchRequest, Theme } from '@shared/models';

/**
 * When the user is authenticated, mirrors language & theme selections back
 * to the server so they follow the user across devices. Anonymous users
 * keep changes in localStorage only.
 */
@Injectable({ providedIn: 'root' })
export class PreferencesSyncService {
  private readonly api = inject(MeApi);
  private readonly auth = inject(AuthService);
  private readonly i18n = inject(I18nService);
  private readonly theme = inject(ThemeService);
  private readonly logger = inject(LoggerService);
  private readonly injector = inject(Injector);

  private lastPushedLanguage: Language | null = null;
  private lastPushedTheme: Theme | null = null;
  private installed = false;

  install(): void {
    if (this.installed) {
      return;
    }
    this.installed = true;
    // effect() requires an injection context (NG0203 otherwise).
    runInInjectionContext(this.injector, () => {
      effect(() => {
        const user = this.auth.user();
        if (!user) {
          this.lastPushedLanguage = null;
          this.lastPushedTheme = null;
          return;
        }
        // On login, adopt server-side preferences without echoing them back.
        untracked(() => {
          void this.i18n.use(user.preferences.language, { persist: false });
          this.theme.setMode(user.preferences.theme, { persist: false });
          this.lastPushedLanguage = user.preferences.language;
          this.lastPushedTheme = user.preferences.theme;
        });
      });

      effect(() => {
        const language = this.i18n.currentLanguage();
        const mode = this.theme.mode();
        if (!this.auth.isAuthenticated()) return;
        if (this.lastPushedLanguage === language && this.lastPushedTheme === mode) return;
        const patch: PreferencesPatchRequest = {
          language,
          theme: mode
        };
        this.lastPushedLanguage = language;
        this.lastPushedTheme = mode;
        // Soft-fail conflicts (optimistic lock / concurrent preference writes).
        this.api.updatePreferences(patch).subscribe({
          error: (err) =>
            this.logger.info('preferences.sync.failed', {
              message: (err as Error).message,
              status: (err as { status?: number })?.status
            })
        });
      });
    });
  }
}
