import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

import { environment } from '@env/environment';
import type { Theme } from '@shared/models';

interface StoredTheme {
  mode: Theme;
}

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly mediaQuery =
    typeof window !== 'undefined' && typeof window.matchMedia === 'function'
      ? window.matchMedia('(prefers-color-scheme: dark)')
      : null;

  private readonly _mode = signal<Theme>('SYSTEM');
  readonly mode = this._mode.asReadonly();
  readonly resolved = computed<'light' | 'dark'>(() => this.compute(this._mode()));

  constructor() {
    const stored = this.readStorage();
    if (stored) {
      this._mode.set(stored.mode);
    }
    this.applyToDocument(this._mode());
    if (this.mediaQuery) {
      const listener = () => {
        if (this._mode() === 'SYSTEM') {
          this.applyToDocument('SYSTEM');
        }
      };
      // addEventListener falls back to deprecated addListener for older Safari.
      if (typeof this.mediaQuery.addEventListener === 'function') {
        this.mediaQuery.addEventListener('change', listener);
      } else if (typeof (this.mediaQuery as { addListener?: unknown }).addListener === 'function') {
        (this.mediaQuery as unknown as { addListener: (l: () => void) => void }).addListener(
          listener
        );
      }
    }
  }

  setMode(mode: Theme, { persist = true }: { persist?: boolean } = {}): void {
    this._mode.set(mode);
    this.applyToDocument(mode);
    if (persist) {
      this.persist(mode);
    }
  }

  toggle(): void {
    const next: Theme = this.resolved() === 'dark' ? 'LIGHT' : 'DARK';
    this.setMode(next);
  }

  private compute(mode: Theme): 'light' | 'dark' {
    if (mode === 'LIGHT') return 'light';
    if (mode === 'DARK') return 'dark';
    return this.mediaQuery?.matches ? 'dark' : 'light';
  }

  private applyToDocument(mode: Theme): void {
    const resolved = this.compute(mode);
    const root = this.document?.documentElement;
    if (root) {
      root.setAttribute('data-theme', resolved);
      root.style.setProperty('color-scheme', resolved);
    }
  }

  private readStorage(): StoredTheme | null {
    if (typeof localStorage === 'undefined') {
      return null;
    }
    try {
      const raw = localStorage.getItem(environment.storageKeys.theme);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as StoredTheme;
      if (parsed?.mode === 'LIGHT' || parsed?.mode === 'DARK' || parsed?.mode === 'SYSTEM') {
        return parsed;
      }
      return null;
    } catch {
      return null;
    }
  }

  private persist(mode: Theme): void {
    if (typeof localStorage === 'undefined') return;
    try {
      localStorage.setItem(environment.storageKeys.theme, JSON.stringify({ mode }));
    } catch {
      /* ignore */
    }
  }
}
