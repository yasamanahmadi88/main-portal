import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

import { MeApi } from '@core/http/api/me.api';
import { ToastService } from '@core/observability/toast.service';
import {
  EmptyStateComponent,
  ErrorStateComponent,
  StatusBadgeComponent,
  SkeletonComponent,
  openConfirmDialog
} from '@shared/ui';
import type { Session } from '@shared/models';

@Component({
  selector: 'app-profile-sessions-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    StatusBadgeComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    SkeletonComponent
  ],
  templateUrl: './profile-sessions-page.component.html',
  styleUrl: './profile-sessions-page.component.scss'
})
export class ProfileSessionsPageComponent {
  private readonly me = inject(MeApi);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly sessions = signal<Session[]>([]);
  readonly busy = signal(false);

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(false);
    try {
      this.sessions.set(await firstValueFrom(this.me.listSessions()));
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  async revoke(session: Session): Promise<void> {
    if (session.current) return;
    if (this.busy()) return;
    this.busy.set(true);
    try {
      await firstValueFrom(this.me.revokeSession(session.id));
      this.toast.success('profile.sessions.revokeSuccess');
      await this.load();
    } finally {
      this.busy.set(false);
    }
  }

  async revokeAll(): Promise<void> {
    const ok = await openConfirmDialog(this.dialog, {
      titleKey: 'profile.sessions.revokeAll',
      messageKey: 'profile.sessions.revokeAll',
      tone: 'danger'
    });
    if (!ok) return;
    const password = prompt('Confirm your password');
    if (!password) return;
    this.busy.set(true);
    try {
      const result = await firstValueFrom(this.me.revokeOtherSessions({ password }));
      this.toast.success('profile.sessions.revokeAllSuccess', { count: result.revokedCount });
      await this.load();
    } finally {
      this.busy.set(false);
    }
  }
}
