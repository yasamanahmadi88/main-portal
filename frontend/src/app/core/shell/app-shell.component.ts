import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  signal,
  viewChild
} from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '@core/authentication/auth.service';
import { PRIMARY_NAV, SECONDARY_NAV, type NavigationItem } from './navigation-items';
import { LanguageSwitcherComponent } from './language-switcher.component';
import { NotificationsMenuComponent } from './notifications-menu.component';
import { ThemeSwitcherComponent } from './theme-switcher.component';
import { UserMenuComponent } from './user-menu.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSidenavModule,
    MatToolbarModule,
    MatTooltipModule,
    TranslatePipe,
    LanguageSwitcherComponent,
    NotificationsMenuComponent,
    ThemeSwitcherComponent,
    UserMenuComponent
  ],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss'
})
export class AppShellComponent {
  private readonly auth = inject(AuthService);
  readonly primaryNav = PRIMARY_NAV;
  readonly secondaryNav = SECONDARY_NAV;
  readonly sidenavOpen = signal(true);
  readonly sidenav = viewChild<ElementRef<HTMLElement>>('sidenav');

  readonly visiblePrimary = computed(() =>
    this.primaryNav.filter((item) => this.canSee(item))
  );

  readonly canSecurityShortcut = computed(() => this.auth.hasPermission('SECURITY_READ'));
  readonly canMonitoringShortcut = computed(() => this.auth.hasPermission('MONITORING_READ'));

  toggleSidenav(): void {
    this.sidenavOpen.update((v) => !v);
  }

  canSee(item: NavigationItem): boolean {
    if (!item.permissions || item.permissions.length === 0) return true;
    return this.auth.hasAnyPermission(item.permissions);
  }

  trackByPath(_index: number, item: NavigationItem): string {
    return item.path;
  }
}
