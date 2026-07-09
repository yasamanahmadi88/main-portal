import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

import { PageHeaderComponent } from '@shared/ui';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatIconModule,
    TranslatePipe,
    PageHeaderComponent
  ],
  template: `
    <app-page-header [title]="'profile.title'" [subtitle]="'profile.subtitle'" />
    <nav class="profile-tabs" aria-label="profile sections">
      <a routerLink="overview" routerLinkActive="profile-tabs__link--active" [routerLinkActiveOptions]="{ exact: false }">
        <mat-icon aria-hidden="true">person</mat-icon>
        <span>{{ 'profile.tabs.overview' | translate }}</span>
      </a>
      <a routerLink="security" routerLinkActive="profile-tabs__link--active">
        <mat-icon aria-hidden="true">shield</mat-icon>
        <span>{{ 'profile.tabs.security' | translate }}</span>
      </a>
      <a routerLink="sessions" routerLinkActive="profile-tabs__link--active">
        <mat-icon aria-hidden="true">devices</mat-icon>
        <span>{{ 'profile.tabs.sessions' | translate }}</span>
      </a>
    </nav>
    <router-outlet />
  `,
  styles: [
    `
      .profile-tabs {
        display: flex;
        gap: var(--app-space-1);
        padding: var(--app-space-1);
        background-color: var(--app-color-surface-sunken);
        border-radius: var(--app-radius-pill);
        margin-block-end: var(--app-space-6);
        overflow-x: auto;
        max-inline-size: max-content;
      }
      .profile-tabs a {
        display: inline-flex;
        align-items: center;
        gap: var(--app-space-2);
        padding: var(--app-space-2) var(--app-space-4);
        border-radius: var(--app-radius-pill);
        color: var(--app-color-text-secondary);
        text-decoration: none;
        font-weight: 500;
      }
      .profile-tabs a:hover { color: var(--app-color-text); text-decoration: none; }
      .profile-tabs__link--active {
        background-color: var(--app-color-surface);
        color: var(--app-color-primary);
        box-shadow: var(--app-shadow-1);
      }
    `
  ]
})
export class ProfilePageComponent {}
