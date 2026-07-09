import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

import { UsersApi } from '@core/http/api/users.api';
import { ToastService } from '@core/observability/toast.service';
import {
  ErrorStateComponent,
  PageHeaderComponent,
  StatusBadgeComponent,
  SkeletonComponent,
  openConfirmDialog
} from '@shared/ui';
import type { User, UserStatus } from '@shared/models';

const STATUS_TONES: Record<UserStatus, 'success' | 'warning' | 'danger' | 'info'> = {
  ACTIVE: 'success',
  PENDING: 'info',
  INACTIVE: 'warning',
  LOCKED: 'danger'
};

@Component({
  selector: 'app-user-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatTooltipModule,
    TranslatePipe,
    PageHeaderComponent,
    StatusBadgeComponent,
    ErrorStateComponent,
    SkeletonComponent
  ],
  templateUrl: './user-detail-page.component.html',
  styleUrl: './user-detail-page.component.scss'
})
export class UserDetailPageComponent {
  private readonly api = inject(UsersApi);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly user = signal<User | null>(null);
  readonly busy = signal(false);
  readonly userId = signal<string | null>(null);

  readonly statusTone = computed<'success' | 'warning' | 'danger' | 'info'>(() => {
    const u = this.user();
    return u ? STATUS_TONES[u.status] : 'info';
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.userId.set(id);
      void this.load(id);
    }
  }

  async load(id: string): Promise<void> {
    this.loading.set(true);
    this.error.set(false);
    try {
      const user = await firstValueFrom(this.api.get(id));
      this.user.set(user);
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  async activate(): Promise<void> {
    await this.run(() => this.api.activate(this.user()!.id), 'users.actions.activate');
  }

  async deactivate(): Promise<void> {
    const ok = await openConfirmDialog(this.dialog, {
      titleKey: 'users.actions.deactivate',
      messageKey: 'users.confirm.deactivate',
      tone: 'danger'
    });
    if (!ok) return;
    await this.run(() => this.api.deactivate(this.user()!.id), 'users.actions.deactivate');
  }

  async unlock(): Promise<void> {
    await this.run(() => this.api.unlock(this.user()!.id), 'users.actions.unlock');
  }

  async triggerPasswordReset(): Promise<void> {
    await this.run(
      () => this.api.triggerPasswordReset(this.user()!.id),
      'users.actions.resetPassword'
    );
  }

  async resetMfa(): Promise<void> {
    await this.run(() => this.api.resetMfa(this.user()!.id), 'users.actions.resetMfa');
  }

  async revokeSessions(): Promise<void> {
    const ok = await openConfirmDialog(this.dialog, {
      titleKey: 'users.actions.revokeSessions',
      messageKey: 'users.confirm.revokeSessions',
      tone: 'danger'
    });
    if (!ok) return;
    await this.run(
      () => this.api.revokeSessions(this.user()!.id),
      'users.actions.revokeSessions'
    );
  }

  private async run<T>(
    action: () => import('rxjs').Observable<T>,
    successKey: string
  ): Promise<void> {
    if (this.busy() || !this.user()) return;
    this.busy.set(true);
    try {
      await firstValueFrom(action());
      this.toast.success(successKey);
      await this.load(this.user()!.id);
    } finally {
      this.busy.set(false);
    }
  }
}
