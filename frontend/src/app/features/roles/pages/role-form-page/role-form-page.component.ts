import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';

import { RolesApi } from '@core/http/api/roles.api';
import { ToastService } from '@core/observability/toast.service';
import { normalizeHttpError } from '@core/error-handling/problem-details';
import { PageHeaderComponent } from '@shared/ui';

@Component({
  selector: 'app-role-form-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TranslatePipe,
    PageHeaderComponent
  ],
  templateUrl: './role-form-page.component.html',
  styleUrl: './role-form-page.component.scss'
})
export class RoleFormPageComponent {
  private readonly api = inject(RolesApi);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly roleId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly editing = computed(() => this.roleId() !== null);

  readonly form = this.fb.nonNullable.group({
    code: [
      '',
      [Validators.required, Validators.pattern(/^[A-Z0-9_]+$/), Validators.maxLength(80)]
    ],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.maxLength(500)]]
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.roleId.set(id);
      this.form.controls.code.disable();
      this.api.get(id).subscribe((role) => {
        this.form.patchValue({
          code: role.code,
          name: role.name,
          description: role.description ?? ''
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
      if (this.editing() && this.roleId()) {
        await firstValueFrom(
          this.api.update(this.roleId()!, {
            name: value.name,
            description: value.description || null
          })
        );
        this.toast.success('roles.form.updateSuccess');
      } else {
        const created = await firstValueFrom(
          this.api.create({
            code: value.code,
            name: value.name,
            description: value.description || undefined
          })
        );
        this.toast.success('roles.form.createSuccess');
        void this.router.navigate(['/roles', created.id]);
        return;
      }
      void this.router.navigate(['/roles', this.roleId()!]);
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
