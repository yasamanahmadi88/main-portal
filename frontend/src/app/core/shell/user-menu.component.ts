import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '@core/authentication/auth.service';

@Component({
  selector: 'app-user-menu',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    MatIconModule,
    TranslatePipe
  ],
  template: `
    <button
      mat-flat-button
      type="button"
      class="app-user-trigger"
      [matMenuTriggerFor]="menu"
      [attr.aria-label]="'common.actions.userMenu' | translate"
    >
      <span class="app-user-avatar" aria-hidden="true">{{ initials() }}</span>
      <span class="app-user-name">{{ displayName() }}</span>
      <mat-icon aria-hidden="true">expand_more</mat-icon>
    </button>
    <mat-menu #menu="matMenu">
      <div class="app-user-header" role="none">
        <div class="app-user-header__name">{{ displayName() }}</div>
        <div class="app-user-header__email">{{ email() }}</div>
      </div>
      <mat-divider />
      <a mat-menu-item routerLink="/profile">
        <mat-icon aria-hidden="true">person</mat-icon>
        <span>{{ 'navigation.profile' | translate }}</span>
      </a>
      <a mat-menu-item routerLink="/profile/sessions">
        <mat-icon aria-hidden="true">devices</mat-icon>
        <span>{{ 'profile.sessions.title' | translate }}</span>
      </a>
      <mat-divider />
      <button mat-menu-item type="button" (click)="signOut()">
        <mat-icon aria-hidden="true">logout</mat-icon>
        <span>{{ 'common.actions.signOut' | translate }}</span>
      </button>
    </mat-menu>
  `,
  styles: [
    `
      .app-user-trigger {
        display: inline-flex;
        gap: var(--app-space-2);
        align-items: center;
        border-radius: var(--app-radius-pill);
        padding-inline: var(--app-space-3);
      }
      .app-user-avatar {
        width: 32px;
        height: 32px;
        border-radius: 999px;
        background: var(--app-color-primary);
        color: var(--app-color-on-primary);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        font-size: 0.875rem;
      }
      .app-user-name {
        font-weight: 500;
      }
      .app-user-header {
        padding: var(--app-space-3) var(--app-space-4);
      }
      .app-user-header__name {
        font-weight: 600;
      }
      .app-user-header__email {
        color: var(--app-color-text-secondary);
        font-size: var(--app-font-size-sm);
      }
      @media (max-width: 640px) {
        .app-user-name {
          display: none;
        }
      }
    `
  ]
})
export class UserMenuComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly user = this.auth.user;
  readonly displayName = computed(() => this.user()?.displayName ?? '');
  readonly email = computed(() => this.user()?.email ?? '');
  readonly initials = computed(() => {
    const name = this.displayName();
    if (!name) return '?';
    const parts = name.trim().split(/\s+/u).slice(0, 2);
    return parts.map((p) => p.charAt(0).toUpperCase()).join('') || '?';
  });

  async signOut(): Promise<void> {
    await this.auth.logout();
    await this.router.navigate(['/auth/login']);
  }
}
