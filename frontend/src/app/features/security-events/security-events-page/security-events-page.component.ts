import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe } from '@ngx-translate/core';

import { SecurityEventsApi, type SecurityEventListQuery } from '@core/http/api/security-events.api';
import { ToastService } from '@core/observability/toast.service';
import {
  EmptyStateComponent,
  ErrorStateComponent,
  PageHeaderComponent,
  StatusBadgeComponent,
  SkeletonComponent,
  openConfirmDialog
} from '@shared/ui';
import type { PageResponse, SecurityEvent, SecurityEventSeverity } from '@shared/models';

interface ListState {
  loading: boolean;
  error: boolean;
  page: PageResponse<SecurityEvent> | null;
}

const SEVERITY_TONES: Record<SecurityEventSeverity, 'info' | 'warning' | 'danger' | 'neutral'> = {
  LOW: 'neutral',
  MEDIUM: 'info',
  HIGH: 'warning',
  CRITICAL: 'danger'
};

@Component({
  selector: 'app-security-events-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    DatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTableModule,
    TranslatePipe,
    PageHeaderComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    SkeletonComponent
  ],
  templateUrl: './security-events-page.component.html',
  styleUrl: './security-events-page.component.scss'
})
export class SecurityEventsPageComponent {
  private readonly api = inject(SecurityEventsApi);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly severity = signal<'' | SecurityEventSeverity>('');
  readonly acknowledged = signal<'' | 'true' | 'false'>('');
  readonly state = signal<ListState>({ loading: true, error: false, page: null });
  readonly columns = ['occurredAt', 'type', 'severity', 'user', 'ip', 'acknowledged', 'actions'];
  readonly severityOptions: readonly SecurityEventSeverity[] = [
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
  ];

  constructor() {
    effect(() => {
      const query: SecurityEventListQuery = {
        page: this.pageIndex(),
        size: this.pageSize(),
        sort: ['occurredAt,desc'],
        severity: this.severity() || undefined,
        acknowledged: this.acknowledged() === '' ? undefined : this.acknowledged() === 'true'
      };
      this.load(query);
    });
  }

  onPage(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
  }

  severityTone(value: SecurityEventSeverity): 'info' | 'warning' | 'danger' | 'neutral' {
    return SEVERITY_TONES[value];
  }

  async acknowledge(event: SecurityEvent): Promise<void> {
    const ok = await openConfirmDialog(this.dialog, {
      titleKey: 'securityEvents.actions.acknowledge',
      messageKey: 'securityEvents.subtitle'
    });
    if (!ok) return;
    await firstValueFrom(this.api.acknowledge(event.id));
    this.toast.success('securityEvents.actions.acknowledgeSuccess');
    this.load({
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: ['occurredAt,desc']
    });
  }

  private load(q: SecurityEventListQuery): void {
    this.state.update((s) => ({ ...s, loading: true, error: false }));
    this.api.list(q).subscribe({
      next: (page) => this.state.set({ loading: false, error: false, page }),
      error: () => this.state.set({ loading: false, error: true, page: null })
    });
  }
}
