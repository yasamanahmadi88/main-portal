import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Directionality } from '@angular/cdk/bidi';
import { TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

import { environment } from '@env/environment';
import type { Language } from '@shared/models';

export interface LanguageDescriptor {
  readonly code: Language;
  readonly nativeName: string;
  readonly englishName: string;
  readonly direction: 'ltr' | 'rtl';
  readonly locale: string;
}

export const SUPPORTED_LANGUAGES: readonly LanguageDescriptor[] = [
  {
    code: 'fa-IR',
    nativeName: 'فارسی',
    englishName: 'Persian',
    direction: 'rtl',
    locale: 'fa-IR'
  },
  {
    code: 'en-US',
    nativeName: 'English',
    englishName: 'English',
    direction: 'ltr',
    locale: 'en-US'
  }
];

const STORAGE_KEY = environment.storageKeys.language;

/**
 * Central language / direction manager.
 *
 * Responsibilities:
 * - Load ngx-translate bundles for the active language.
 * - Sync `<html lang>` and `<html dir>` so CSS logical properties and
 *   third-party components (Material CDK) work correctly.
 * - Persist the anonymous user's choice in localStorage; authenticated
 *   preferences are synced separately by the {@link PreferencesService}.
 */
@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly translate = inject(TranslateService);
  private readonly directionality = inject(Directionality);
  private readonly document = inject(DOCUMENT);

  private readonly _current = signal<Language>(environment.defaultLanguage as Language);
  readonly currentLanguage = this._current.asReadonly();
  readonly languages = SUPPORTED_LANGUAGES;
  readonly currentDescriptor = computed(
    () =>
      SUPPORTED_LANGUAGES.find((l) => l.code === this._current()) ??
      SUPPORTED_LANGUAGES[0]!
  );
  readonly direction = computed(() => this.currentDescriptor().direction);
  readonly isRtl = computed(() => this.direction() === 'rtl');

  /** Set up defaults before Angular bootstrap. */
  initialize(): Promise<void> {
    const preferred = this.resolveInitialLanguage();
    this.translate.addLangs(SUPPORTED_LANGUAGES.map((l) => l.code));
    this.translate.setFallbackLang('en-US');
    return this.applyLanguage(preferred, { persist: false });
  }

  /** Change language at runtime (also updates <html> attributes). */
  async use(language: Language, options: { persist?: boolean } = {}): Promise<void> {
    if (language === this._current()) {
      return;
    }
    await this.applyLanguage(language, { persist: options.persist ?? true });
  }

  /** Utility for the change-password screen etc. */
  instant<T = string>(key: string, params?: Record<string, unknown>): T {
    return this.translate.instant(key, params) as T;
  }

  private resolveInitialLanguage(): Language {
    if (typeof localStorage === 'undefined') {
      return environment.defaultLanguage as Language;
    }
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored && this.isSupported(stored)) {
      return stored as Language;
    }
    // Product default is Persian (fa-IR). Do not infer from navigator.language —
    // CI browsers and many developer machines report en-US and would otherwise
    // override the documented portal default on first visit.
    return environment.defaultLanguage as Language;
  }

  private isSupported(code: string): boolean {
    return SUPPORTED_LANGUAGES.some((l) => l.code === code);
  }

  private async applyLanguage(
    language: Language,
    { persist }: { persist: boolean }
  ): Promise<void> {
    await firstValueFrom(this.translate.use(language));
    this._current.set(language);
    const descriptor = SUPPORTED_LANGUAGES.find((l) => l.code === language)!;
    if (this.document?.documentElement) {
      this.document.documentElement.setAttribute('lang', language);
      this.document.documentElement.setAttribute('dir', descriptor.direction);
    }
    // Notify CDK-aware components of a direction change. `value` is
    // read-only on newer CDK builds so we mutate the private backing field
    // when it exists; consumers should rely on `change` for reactive updates.
    this.directionality.change?.emit(descriptor.direction);
    try {
      const writable = this.directionality as unknown as { value: 'ltr' | 'rtl' };
      writable.value = descriptor.direction;
    } catch {
      /* CDK has locked `value` down; the change emitter is the source of truth */
    }
    if (persist && typeof localStorage !== 'undefined') {
      try {
        localStorage.setItem(STORAGE_KEY, language);
      } catch {
        /* localStorage may be blocked in private modes */
      }
    }
  }
}
