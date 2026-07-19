import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal
} from '@angular/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

import type { Permission, PermissionMatrix, Role } from '@shared/models';

interface CellKey {
  readonly roleId: string;
  readonly permissionCode: string;
}

@Component({
  selector: 'app-permission-matrix',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatCheckboxModule, MatIconModule, TranslatePipe],
  template: `
    <div class="matrix" role="table" [attr.aria-label]="'permissions.matrix.title' | translate">
      <div class="matrix__scroll">
        <table>
          <thead>
            <tr>
              <th scope="col" class="matrix__perm-header">
                {{ 'permissions.headers.permission' | translate }}
              </th>
              @for (role of roles(); track role.id) {
                <th scope="col" class="matrix__role-header" [title]="role.name">
                  <div class="matrix__role-name">{{ role.name }}</div>
                  <div class="matrix__role-code">{{ role.code }}</div>
                </th>
              }
            </tr>
          </thead>
          <tbody>
            @for (permission of permissions(); track permission.code) {
              <tr>
                <th scope="row" class="matrix__perm">
                  <div class="matrix__perm-code">{{ permission.code }}</div>
                  <div class="matrix__perm-desc">{{ permission.description }}</div>
                </th>
                @for (role of roles(); track role.id) {
                  <td class="matrix__cell">
                    <mat-checkbox
                      [checked]="isChecked(role.id, permission.code)"
                      [disabled]="readonly() || role.system"
                      (change)="toggle(role.id, permission.code, $event.checked)"
                      [attr.aria-label]="permission.code + ' / ' + role.code"
                    />
                  </td>
                }
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [
    `
      .matrix {
        border: 1px solid var(--app-color-outline);
        border-radius: var(--app-radius-lg);
        background: var(--app-color-surface);
        overflow: hidden;
      }
      .matrix__scroll {
        overflow: auto;
        max-inline-size: 100%;
      }
      table {
        border-collapse: collapse;
        min-inline-size: 100%;
        font-size: var(--app-font-size-sm);
      }
      th, td {
        padding: var(--app-space-2) var(--app-space-3);
        border-block-end: 1px solid var(--app-color-divider);
        text-align: start;
        vertical-align: middle;
      }
      thead th {
        background-color: var(--app-color-surface-sunken);
        position: sticky;
        inset-block-start: 0;
        z-index: 1;
      }
      .matrix__perm-header {
        min-inline-size: 260px;
      }
      .matrix__role-header {
        min-inline-size: 140px;
      }
      .matrix__role-name { font-weight: 600; }
      .matrix__role-code { color: var(--app-color-text-muted); font-size: var(--app-font-size-xs); }
      .matrix__perm {
        text-align: start;
        position: sticky;
        inset-inline-start: 0;
        background-color: var(--app-color-surface);
      }
      .matrix__perm-code {
        font-family: var(--app-font-family-mono);
        font-weight: 600;
      }
      .matrix__perm-desc {
        color: var(--app-color-text-secondary);
        font-size: var(--app-font-size-xs);
      }
      .matrix__cell {
        text-align: center;
      }
    `
  ]
})
export class PermissionMatrixComponent {
  readonly matrix = input.required<PermissionMatrix>();
  readonly readonly = input<boolean>(false);
  readonly changed = output<CellKey & { assigned: boolean }>();

  readonly roles = computed<Role[]>(() => this.matrix().roles);
  readonly permissions = computed<Permission[]>(() => this.matrix().permissions);

  private readonly overrides = signal<Map<string, boolean>>(new Map());

  isChecked(roleId: string, permissionCode: string): boolean {
    const key = `${roleId}::${permissionCode}`;
    const override = this.overrides().get(key);
    if (override !== undefined) return override;
    return this.matrix().assignments.some(
      (a) => a.roleId === roleId && a.permissionCode === permissionCode
    );
  }

  toggle(roleId: string, permissionCode: string, assigned: boolean): void {
    const key = `${roleId}::${permissionCode}`;
    this.overrides.update((current) => {
      const next = new Map(current);
      next.set(key, assigned);
      return next;
    });
    this.changed.emit({ roleId, permissionCode, assigned });
  }
}
