import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '@core/authentication/auth.service';
import { PasswordInputComponent } from '@shared/ui';

function matchValidator(otherControlName: string) {
  return (control: AbstractControl): ValidationErrors | null => {
    const other = control.parent?.get(otherControlName);
    if (!other) return null;
    return other.value === control.value ? null : { mismatch: true };
  };
}

@Component({
  selector: 'app-reset-password-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    TranslatePipe,
    PasswordInputComponent
  ],
  templateUrl: './reset-password-page.component.html',
  styleUrl: './reset-password-page.component.scss'
})
export class ResetPasswordPageComponent {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  readonly submitting = signal(false);
  readonly done = signal(false);
  readonly errorKey = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    token: [this.route.snapshot.queryParamMap.get('token') ?? '', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(12)]],
    confirmPassword: ['', [Validators.required, matchValidator('newPassword')]]
  });

  readonly confirmMismatch = computed(() =>
    this.form.controls.confirmPassword.touched &&
    this.form.controls.confirmPassword.hasError('mismatch')
  );

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.errorKey.set(null);
    try {
      const { token, newPassword } = this.form.getRawValue();
      await this.auth.resetPassword({ token, newPassword });
      this.done.set(true);
    } catch (err) {
      if (err instanceof HttpErrorResponse) {
        if (err.status === 400 || err.status === 404) {
          this.errorKey.set('authentication.errors.resetTokenInvalid');
        } else if (err.status === 422) {
          this.errorKey.set('validation.passwordStrength');
        } else if (err.status >= 500) {
          this.errorKey.set('common.errors.server');
        } else {
          this.errorKey.set('common.errors.server');
        }
      } else {
        this.errorKey.set('common.errors.server');
      }
    } finally {
      this.submitting.set(false);
    }
  }
}
