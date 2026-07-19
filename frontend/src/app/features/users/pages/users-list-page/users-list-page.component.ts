import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

import { UsersApi, type UserListQuery } from '@core/http/api/users.api';
import {
  EmptyStateComponent,
  ErrorStateComponent,
  PageHeaderComponent,
  StatusBadgeComponent,
  SkeletonComponent
} from '@shared/ui';
import type { PageResponse, User, UserStatus } from '@shared/models';

interface ListState {
  loading: boolean;
  error: boolean;
  page: PageResponse<User> | null;
}

const STATUS_TONES: Record<UserStatus, 'success' | 'warning' | 'danger' | 'info'> = {
  ACTIVE: 'success',
  PENDING: 'info',
  INACTIVE: 'warning',
  LOCKED: 'danger'
};

@Component({
  selector: 'app-users-list-page',
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
    MatMenuModule,
    MatPaginatorModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    TranslatePipe,
    PageHeaderComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    SkeletonComponent
  ],
  templateUrl: './users-list-page.component.html',
  styleUrl: './users-list-page.component.scss'
})
export class UsersListPageComponent {
  private readonly api = inject(UsersApi);

  readonly search = signal('');
  readonly status = signal<UserStatus | ''>('');
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly sort = signal<string>('displayName,asc');

  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private lastToken = 0;

  private readonly query = computed<UserListQuery>(() => ({
    page: this.pageIndex(),
    size: this.pageSize(),
    sort: [this.sort()],
    search: this.search() || undefined,
    status: this.status() || undefined
  }));

  readonly state = signal<ListState>({ loading: true, error: false, page: null });

  readonly columns = ['user', 'email', 'status', 'roles', 'lastLogin', 'actions'];
  readonly statusOptions: readonly UserStatus[] = ['ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING'];

  constructor() {
    effect(() => {
      const q = this.query();
      this.load(q);
    });
  }

  reload(): void {
    this.load(this.query());
  }

  searchChanged(value: string): void {
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      this.pageIndex.set(0);
      this.search.set(value);
    }, 300);
  }

  statusChanged(value: UserStatus | ''): void {
    this.pageIndex.set(0);
    this.status.set(value);
  }

  onPage(event: PageEvent): void {
    if (event.pageSize !== this.pageSize()) this.pageIndex.set(0);
    else this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  onSort(event: Sort): void {
    const dir = event.direction || 'asc';
    if (event.active) {
      this.sort.set(`${event.active},${dir}`);
    }
  }

  statusTone(status: UserStatus): 'success' | 'warning' | 'danger' | 'info' {
    return STATUS_TONES[status];
  }

  private load(q: UserListQuery): void {
    const token = ++this.lastToken;
    this.state.update((s) => ({ ...s, loading: true, error: false }));
    this.api.list(q).subscribe({
      next: (page) => {
        if (token !== this.lastToken) return;
        this.state.set({ loading: false, error: false, page });
      },
      error: () => {
        if (token !== this.lastToken) return;
        this.state.set({ loading: false, error: true, page: null });
      }
    });
  }
}
