import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AuditApi, type AuditListQuery } from '@core/http/api/audit.api';
import { ToastService } from '@core/observability/toast.service';
import {
  EmptyStateComponent,
  ErrorStateComponent,
  PageHeaderComponent,
  StatusBadgeComponent,
  SkeletonComponent
} from '@shared/ui';
import type { AuditEvent, PageResponse } from '@shared/models';

interface ListState {
  loading: boolean;
  error: boolean;
  page: PageResponse<AuditEvent> | null;
}

@Component({
  selector: 'app-audit-list-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
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
  templateUrl: './audit-list-page.component.html',
  styleUrl: './audit-list-page.component.scss'
})
export class AuditListPageComponent {
  private readonly api = inject(AuditApi);
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);

  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly outcome = signal<'' | 'SUCCESS' | 'FAILURE'>('');
  readonly action = signal('');
  readonly state = signal<ListState>({ loading: true, error: false, page: null });
  readonly verifying = signal(false);
  readonly columns = ['when', 'actor', 'action', 'resource', 'outcome', 'ip'];

  constructor() {
    effect(() => {
      const query: AuditListQuery = {
        page: this.pageIndex(),
        size: this.pageSize(),
        sort: ['occurredAt,desc'],
        outcome: this.outcome() || undefined,
        action: this.action() || undefined
      };
      this.load(query);
    });
  }

  onPage(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
  }

  async verifyIntegrity(): Promise<void> {
    this.verifying.set(true);
    try {
      const result = await firstValueFrom(this.api.verifyIntegrity());
      if (result.valid) {
        this.toast.success('audit.actions.verifySuccess', { count: result.checkedEvents });
      } else {
        this.toast.warning('audit.actions.verifyFailure', {
          id: result.firstInvalidEventId ?? 'unknown'
        });
      }
    } finally {
      this.verifying.set(false);
    }
  }

  private load(q: AuditListQuery): void {
    this.state.update((s) => ({ ...s, loading: true, error: false }));
    this.api.list(q).subscribe({
      next: (page) => this.state.set({ loading: false, error: false, page }),
      error: () => this.state.set({ loading: false, error: true, page: null })
    });
  }
}
