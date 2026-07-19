import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

import { RolesApi } from '@core/http/api/roles.api';
import { ToastService } from '@core/observability/toast.service';
import {
  ErrorStateComponent,
  PageHeaderComponent,
  StatusBadgeComponent,
  SkeletonComponent,
  openConfirmDialog
} from '@shared/ui';
import type { Role } from '@shared/models';

@Component({
  selector: 'app-role-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    PageHeaderComponent,
    StatusBadgeComponent,
    ErrorStateComponent,
    SkeletonComponent
  ],
  templateUrl: './role-detail-page.component.html',
  styleUrl: './role-detail-page.component.scss'
})
export class RoleDetailPageComponent {
  private readonly api = inject(RolesApi);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly role = signal<Role | null>(null);
  readonly roleId = signal<string | null>(null);

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.roleId.set(id);
      void this.load(id);
    }
  }

  async load(id: string): Promise<void> {
    this.loading.set(true);
    this.error.set(false);
    try {
      const role = await firstValueFrom(this.api.get(id));
      this.role.set(role);
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  async remove(): Promise<void> {
    const role = this.role();
    if (!role || role.system) return;
    const ok = await openConfirmDialog(this.dialog, {
      titleKey: 'common.actions.delete',
      messageKey: 'roles.confirm.delete',
      tone: 'danger'
    });
    if (!ok) return;
    await firstValueFrom(this.api.delete(role.id));
    this.toast.success('roles.form.deleteSuccess');
    void this.router.navigate(['/roles']);
  }
}
