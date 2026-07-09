import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { catchError, forkJoin, of, switchMap } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';

import { RolesApi } from '@core/http/api/roles.api';
import { UsersApi } from '@core/http/api/users.api';
import { ToastService } from '@core/observability/toast.service';
import { normalizeHttpError } from '@core/error-handling/problem-details';
import { PageHeaderComponent } from '@shared/ui';
import type { Role } from '@shared/models';

@Component({
  selector: 'app-user-form-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    TranslatePipe,
    PageHeaderComponent
  ],
  templateUrl: './user-form-page.component.html',
  styleUrl: './user-form-page.component.scss'
})
export class UserFormPageComponent {
  private readonly users = inject(UsersApi);
  private readonly roles = inject(RolesApi);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly userId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly fieldErrors = signal<Record<string, string>>({});

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email]],
    displayName: ['', [Validators.required, Validators.maxLength(160)]],
    roleIds: this.fb.nonNullable.control<string[]>([]),
    sendInvite: [true]
  });

  readonly allRoles = toSignal(
    this.roles.list({ page: 0, size: 200, sort: ['name,asc'] }).pipe(
      catchError(() => of({ content: [] as Role[] } as { content: Role[] })),
      switchMap((page) => of(page.content))
    ),
    { initialValue: [] as Role[] }
  );

  readonly editing = computed(() => this.userId() !== null);

  constructor() {
    const idFromRoute = this.route.snapshot.paramMap.get('id');
    if (idFromRoute) {
      this.userId.set(idFromRoute);
      this.form.controls.username.disable();
      this.form.controls.sendInvite.disable();
      forkJoin({
        user: this.users.get(idFromRoute),
        roles: this.users.getRoles(idFromRoute).pipe(catchError(() => of([] as Role[])))
      }).subscribe(({ user, roles }) => {
        this.form.patchValue({
          username: user.username,
          email: user.email,
          displayName: user.displayName,
          roleIds: roles.map((r) => r.id)
        });
      });
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.fieldErrors.set({});
    const value = this.form.getRawValue();
    try {
      if (this.editing() && this.userId()) {
        const updated = await this.users
          .update(this.userId()!, {
            email: value.email,
            displayName: value.displayName
          })
          .toPromise();
        await this.users
          .replaceRoles(this.userId()!, { roleIds: value.roleIds })
          .toPromise();
        this.toast.success('users.form.updateSuccess');
        if (updated) {
          void this.router.navigate(['/users', updated.id]);
        }
      } else {
        const created = await this.users
          .create({
            username: value.username,
            email: value.email,
            displayName: value.displayName,
            roleIds: value.roleIds,
            sendInvite: value.sendInvite
          })
          .toPromise();
        this.toast.success('users.form.createSuccess');
        if (created) {
          void this.router.navigate(['/users', created.id]);
        }
      }
    } catch (err) {
      if (err instanceof HttpErrorResponse) {
        const normalized = normalizeHttpError(err);
        this.fieldErrors.set(normalized.fieldErrors);
      }
    } finally {
      this.submitting.set(false);
    }
  }
}
