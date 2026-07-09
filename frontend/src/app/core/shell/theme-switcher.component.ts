import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

import { ThemeService } from '@core/i18n/theme.service';
import type { Theme } from '@shared/models';

interface ThemeOption {
  readonly mode: Theme;
  readonly labelKey: string;
  readonly icon: string;
}

const OPTIONS: readonly ThemeOption[] = [
  { mode: 'LIGHT', labelKey: 'common.theme.light', icon: 'light_mode' },
  { mode: 'DARK', labelKey: 'common.theme.dark', icon: 'dark_mode' },
  { mode: 'SYSTEM', labelKey: 'common.theme.system', icon: 'devices' }
];

@Component({
  selector: 'app-theme-switcher',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatMenuModule, MatIconModule, MatTooltipModule, TranslatePipe],
  template: `
    <button
      mat-stroked-button
      type="button"
      [matMenuTriggerFor]="menu"
      [matTooltip]="'common.actions.changeTheme' | translate"
      [attr.aria-label]="'common.actions.changeTheme' | translate"
    >
      <mat-icon aria-hidden="true">{{ resolvedIcon() }}</mat-icon>
      <span class="app-theme-label">{{ 'common.theme.' + resolved() | translate }}</span>
    </button>
    <mat-menu #menu="matMenu">
      @for (option of options; track option.mode) {
        <button
          mat-menu-item
          type="button"
          (click)="select(option.mode)"
          [attr.aria-current]="option.mode === mode() ? 'true' : null"
        >
          <mat-icon aria-hidden="true">{{ option.icon }}</mat-icon>
          <span>{{ option.labelKey | translate }}</span>
        </button>
      }
    </mat-menu>
  `,
  styles: [
    `
      .app-theme-label {
        margin-inline-start: var(--app-space-2);
      }
    `
  ]
})
export class ThemeSwitcherComponent {
  private readonly theme = inject(ThemeService);
  readonly options = OPTIONS;
  readonly mode = this.theme.mode;
  readonly resolved = this.theme.resolved;

  resolvedIcon(): string {
    return this.resolved() === 'dark' ? 'dark_mode' : 'light_mode';
  }

  select(mode: Theme): void {
    this.theme.setMode(mode);
  }
}
