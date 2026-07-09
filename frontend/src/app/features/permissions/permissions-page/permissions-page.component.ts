import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslatePipe } from '@ngx-translate/core';

import { PermissionsApi, RolesApi } from '@core/http/api/roles.api';
import {
  EmptyStateComponent,
  ErrorStateComponent,
  PageHeaderComponent,
  PermissionMatrixComponent,
  StatusBadgeComponent,
  SkeletonComponent
} from '@shared/ui';
import type { Permission, PermissionMatrix, Role } from '@shared/models';

interface State {
  loading: boolean;
  error: boolean;
  permissions: Permission[];
  matrix: PermissionMatrix | null;
}

@Component({
  selector: 'app-permissions-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatTabsModule,
    TranslatePipe,
    PageHeaderComponent,
    PermissionMatrixComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    SkeletonComponent
  ],
  templateUrl: './permissions-page.component.html',
  styleUrl: './permissions-page.component.scss'
})
export class PermissionsPageComponent {
  private readonly permissions = inject(PermissionsApi);
  readonly state = signal<State>({
    loading: true,
    error: false,
    permissions: [],
    matrix: null
  });

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.state.update((s) => ({ ...s, loading: true, error: false }));
    forkJoin({
      list: this.permissions.list({ page: 0, size: 500, sort: ['code,asc'] }).pipe(
        catchError(() => of({ content: [] as Permission[] } as { content: Permission[] }))
      ),
      matrix: this.permissions.getMatrix().pipe(
        catchError(() =>
          of<PermissionMatrix>({ roles: [], permissions: [], assignments: [] })
        )
      )
    }).subscribe({
      next: ({ list, matrix }) => {
        this.state.set({
          loading: false,
          error: false,
          permissions: list.content,
          matrix
        });
      },
      error: () =>
        this.state.set({
          loading: false,
          error: true,
          permissions: [],
          matrix: null
        })
    });
  }
}
