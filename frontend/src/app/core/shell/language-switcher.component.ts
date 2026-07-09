import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

import { I18nService, SUPPORTED_LANGUAGES } from '@core/i18n/i18n.service';
import type { Language } from '@shared/models';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatMenuModule, MatIconModule, MatTooltipModule, TranslatePipe],
  template: `
    <button
      mat-stroked-button
      type="button"
      class="app-language-trigger"
      data-testid="language-switcher"
      [matMenuTriggerFor]="menu"
      [matTooltip]="'common.actions.changeLanguage' | translate"
      [attr.aria-label]="'common.actions.changeLanguage' | translate"
    >
      <mat-icon aria-hidden="true">translate</mat-icon>
      <span>{{ currentLabel() }}</span>
    </button>
    <mat-menu #menu="matMenu">
      @for (lang of languages; track lang.code) {
        <button
          mat-menu-item
          type="button"
          (click)="select(lang.code)"
          [attr.aria-current]="lang.code === current() ? 'true' : null"
        >
          <span>{{ lang.nativeName }}</span>
          @if (lang.code === current()) {
            <mat-icon aria-hidden="true">check</mat-icon>
          }
        </button>
      }
    </mat-menu>
  `,
  styles: [
    `
      .app-language-trigger {
        display: inline-flex;
        align-items: center;
        gap: var(--app-space-2);
        border-radius: var(--app-radius-md);
      }
    `
  ]
})
export class LanguageSwitcherComponent {
  private readonly i18n = inject(I18nService);
  readonly languages = SUPPORTED_LANGUAGES;
  readonly current = this.i18n.currentLanguage;
  readonly currentLabel = computed(() => this.i18n.currentDescriptor().nativeName);

  select(code: Language): void {
    void this.i18n.use(code);
  }
}
