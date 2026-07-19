import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith, timer, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';

import { DashboardApi } from '@core/http/api/dashboard.api';
import { EmptyStateComponent, PageHeaderComponent, SkeletonComponent } from '@shared/ui';
import type { DashboardSummary } from '@shared/models';

interface DashboardState {
  status: 'loading' | 'ready' | 'error';
  summary: DashboardSummary | null;
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DecimalPipe,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslatePipe,
    PageHeaderComponent,
    SkeletonComponent,
    EmptyStateComponent
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent {
  private readonly api = inject(DashboardApi);
  readonly refreshTick = signal(0);

  private readonly state$ = timer(0, 60_000).pipe(
    switchMap(() =>
      this.api.summary().pipe(
        map<DashboardSummary, DashboardState>((summary) => ({ status: 'ready', summary })),
        catchError(() => of<DashboardState>({ status: 'error', summary: null }))
      )
    ),
    startWith<DashboardState>({ status: 'loading', summary: null })
  );

  readonly state = toSignal(this.state$, {
    initialValue: { status: 'loading', summary: null } satisfies DashboardState
  });
}
