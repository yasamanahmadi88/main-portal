import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { PercentPipe } from '@angular/common';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, forkJoin, map, of, startWith, switchMap, timer } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '@core/authentication/auth.service';
import { MonitoringApi } from '@core/http/api/monitoring.api';
import {
  EmptyStateComponent,
  PageHeaderComponent,
  SkeletonComponent,
  StatusBadgeComponent
} from '@shared/ui';
import type {
  MonitoringMetricsSummary,
  MonitoringOverview,
  MonitoringPerformance
} from '@shared/models';

interface MonitoringState {
  status: 'loading' | 'ready' | 'error';
  overview: MonitoringOverview | null;
  performance: MonitoringPerformance | null;
}

@Component({
  selector: 'app-monitoring-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    PercentPipe,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    PageHeaderComponent,
    SkeletonComponent,
    EmptyStateComponent,
    StatusBadgeComponent
  ],
  templateUrl: './monitoring-page.component.html',
  styleUrl: './monitoring-page.component.scss'
})
export class MonitoringPageComponent {
  private readonly api = inject(MonitoringApi);
  private readonly auth = inject(AuthService);

  readonly refreshTick = signal(0);

  private readonly state$ = combineLatest([timer(0, 60_000), toObservable(this.refreshTick)]).pipe(
    switchMap(() => {
      const canMetrics = this.auth.hasPermission('MONITORING_METRICS_READ');
      return forkJoin({
        overview: this.api.overview(),
        metrics: canMetrics
          ? this.api.metrics().pipe(catchError(() => of(null as MonitoringMetricsSummary | null)))
          : of(null as MonitoringMetricsSummary | null)
      }).pipe(
        map(
          ({ overview, metrics }): MonitoringState => ({
            status: 'ready',
            overview,
            performance:
              metrics?.performance ??
              ({
                uptimeSeconds: overview.jvm.uptimeSeconds
              } satisfies MonitoringPerformance)
          })
        ),
        catchError(() =>
          of<MonitoringState>({ status: 'error', overview: null, performance: null })
        ),
        startWith<MonitoringState>({ status: 'loading', overview: null, performance: null })
      );
    })
  );

  readonly state = toSignal(this.state$, {
    initialValue: { status: 'loading', overview: null, performance: null } satisfies MonitoringState
  });

  readonly generatedAt = computed(() => this.state().overview?.generatedAt ?? null);
  readonly grafanaUrl = computed(() => this.state().overview?.links?.['grafana'] ?? null);
  readonly prometheusUrl = computed(() => this.state().overview?.links?.['prometheus'] ?? null);

  statusTone(status: string | undefined): 'success' | 'danger' | 'warning' | 'neutral' {
    const normalized = (status ?? '').toUpperCase();
    if (normalized === 'UP' || normalized === 'OK') return 'success';
    if (normalized === 'DOWN') return 'danger';
    if (normalized === 'UNKNOWN') return 'warning';
    return 'neutral';
  }

  formatBytes(bytes: number | undefined): string {
    if (bytes == null || Number.isNaN(bytes)) return '—';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let value = bytes;
    let unit = 0;
    while (value >= 1024 && unit < units.length - 1) {
      value /= 1024;
      unit += 1;
    }
    return `${value.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
  }

  formatUptime(seconds: number | undefined): string {
    if (seconds == null) return '—';
    const days = Math.floor(seconds / 86_400);
    const hours = Math.floor((seconds % 86_400) / 3_600);
    const mins = Math.floor((seconds % 3_600) / 60);
    if (days > 0) return `${days}d ${hours}h`;
    if (hours > 0) return `${hours}h ${mins}m`;
    return `${mins}m`;
  }

  reload(): void {
    this.refreshTick.update((n) => n + 1);
  }
}
