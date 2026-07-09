import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '@core/authentication/auth.service';
import { normalizeHttpError } from '@core/error-handling/problem-details';

@Component({
  selector: 'app-forgot-password-page',
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
    TranslatePipe
  ],
  templateUrl: './forgot-password-page.component.html',
  styleUrl: './forgot-password-page.component.scss'
})
export class ForgotPasswordPageComponent {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly errorKey = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.errorKey.set(null);
    try {
      await this.auth.forgotPassword(this.form.getRawValue());
      this.submitted.set(true);
    } catch (err) {
      // Backend responds 204 even if the email doesn't exist; only network
      // failures should surface as errors.
      if (err instanceof HttpErrorResponse) {
        if (err.status === 0) {
          this.errorKey.set('common.errors.network');
        } else if (err.status >= 500) {
          this.errorKey.set('common.errors.server');
        } else if (err.status === 429) {
          this.errorKey.set('common.errors.rateLimited');
        } else {
          const normalized = normalizeHttpError(err);
          this.errorKey.set(normalized.title ?? 'common.errors.server');
        }
      } else {
        this.errorKey.set('common.errors.server');
      }
    } finally {
      this.submitting.set(false);
    }
  }
}
