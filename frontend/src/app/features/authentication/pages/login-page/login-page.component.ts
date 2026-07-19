import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '@core/authentication/auth.service';
import { messageKeyForError, normalizeHttpError } from '@core/error-handling/problem-details';
import { PasswordInputComponent } from '@shared/ui';

@Component({
  selector: 'app-login-page',
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
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(160)]],
    password: ['', [Validators.required, Validators.minLength(1)]]
  });

  readonly submitting = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly errorDetail = signal<string | null>(null);
  readonly hasError = computed(() => this.errorKey() !== null || this.errorDetail() !== null);

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.errorKey.set(null);
    this.errorDetail.set(null);
    try {
      // rememberDevice is reserved for a future trusted-device feature; never advertise a stub UI.
      const { username, password } = this.form.getRawValue();
      const response = await this.auth.login({ username, password, rememberDevice: false });
      if (response.status === 'MFA_REQUIRED') {
        await this.router.navigate(['/auth/mfa']);
        return;
      }
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
      await this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : '/dashboard');
    } catch (err) {
      if (err instanceof HttpErrorResponse) {
        const normalized = normalizeHttpError(err);
        const key = messageKeyForError(normalized);
        if (key) {
          this.errorKey.set(key);
        } else if (normalized.status === 0) {
          this.errorKey.set('common.errors.network');
        } else if (normalized.status >= 500) {
          this.errorKey.set('common.errors.server');
        } else {
          this.errorKey.set('authentication.errors.invalidCredentials');
          this.errorDetail.set(normalized.message);
        }
      } else {
        this.errorKey.set('common.errors.server');
      }
    } finally {
      this.submitting.set(false);
    }
  }
}
