import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * Placeholder for future SSE-driven notifications; keeps the shell
 * complete so the topbar layout is stable when the feed lights up.
 */
@Component({
  selector: 'app-notifications-menu',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatBadgeModule,
    MatTooltipModule,
    TranslatePipe
  ],
  template: `
    <button
      mat-icon-button
      type="button"
      [matTooltip]="'common.actions.notifications' | translate"
      [matMenuTriggerFor]="menu"
      [attr.aria-label]="'common.actions.notifications' | translate"
    >
      <mat-icon aria-hidden="true">notifications</mat-icon>
    </button>
    <mat-menu #menu="matMenu" class="app-notifications-panel">
      <div class="app-notifications" role="none">
        <h3>{{ 'common.notifications.title' | translate }}</h3>
        <p>{{ 'common.notifications.empty' | translate }}</p>
      </div>
    </mat-menu>
  `,
  styles: [
    `
      .app-notifications {
        min-width: 260px;
        padding: var(--app-space-4);
      }
      .app-notifications h3 {
        margin: 0 0 var(--app-space-2);
        font-size: var(--app-font-size-md);
      }
      .app-notifications p {
        margin: 0;
        color: var(--app-color-text-secondary);
        font-size: var(--app-font-size-sm);
      }
    `
  ]
})
export class NotificationsMenuComponent {}
