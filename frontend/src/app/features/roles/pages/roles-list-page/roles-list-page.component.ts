import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

import { RolesApi } from '@core/http/api/roles.api';
import {
  EmptyStateComponent,
  ErrorStateComponent,
  PageHeaderComponent,
  StatusBadgeComponent,
  SkeletonComponent
} from '@shared/ui';
import type { PageResponse, Role } from '@shared/models';

interface ListState {
  loading: boolean;
  error: boolean;
  page: PageResponse<Role> | null;
}

@Component({
  selector: 'app-roles-list-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatPaginatorModule,
    MatTableModule,
    MatTooltipModule,
    TranslatePipe,
    PageHeaderComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    SkeletonComponent
  ],
  templateUrl: './roles-list-page.component.html',
  styleUrl: './roles-list-page.component.scss'
})
export class RolesListPageComponent {
  private readonly api = inject(RolesApi);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly state = signal<ListState>({ loading: true, error: false, page: null });
  readonly columns = ['code', 'name', 'description', 'permissions', 'system', 'updatedAt'];

  constructor() {
    effect(() => {
      this.load(this.pageIndex(), this.pageSize());
    });
  }

  reload(): void {
    this.load(this.pageIndex(), this.pageSize());
  }

  onPage(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
  }

  private load(page: number, size: number): void {
    this.state.update((s) => ({ ...s, loading: true, error: false }));
    this.api.list({ page, size, sort: ['name,asc'] }).subscribe({
      next: (result) => this.state.set({ loading: false, error: false, page: result }),
      error: () => this.state.set({ loading: false, error: true, page: null })
    });
  }
}
