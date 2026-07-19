import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, JsonPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

import { AuditApi } from '@core/http/api/audit.api';
import {
  ErrorStateComponent,
  PageHeaderComponent,
  StatusBadgeComponent,
  SkeletonComponent
} from '@shared/ui';
import type { AuditEvent } from '@shared/models';

@Component({
  selector: 'app-audit-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    JsonPipe,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    PageHeaderComponent,
    StatusBadgeComponent,
    ErrorStateComponent,
    SkeletonComponent
  ],
  templateUrl: './audit-detail-page.component.html',
  styleUrl: './audit-detail-page.component.scss'
})
export class AuditDetailPageComponent {
  private readonly api = inject(AuditApi);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly event = signal<AuditEvent | null>(null);
  readonly eventId = signal<string | null>(null);

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.eventId.set(id);
      void this.load(id);
    }
  }

  async load(id: string): Promise<void> {
    this.loading.set(true);
    this.error.set(false);
    try {
      this.event.set(await firstValueFrom(this.api.get(id)));
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }
}
