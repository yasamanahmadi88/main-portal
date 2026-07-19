import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
  viewChild
} from '@angular/core';
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
import { CaptchaChallengeComponent } from '@features/authentication/components/captcha-challenge/captcha-challenge.component';
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
    PasswordInputComponent,
    CaptchaChallengeComponent
  ],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly captcha = viewChild(CaptchaChallengeComponent);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(160)]],
    password: ['', [Validators.required, Validators.minLength(1)]],
    captchaId: ['', [Validators.required]],
    captchaAnswer: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(12)]]
  });

  readonly submitting = signal(false);
  readonly captchaReady = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly errorDetail = signal<string | null>(null);
  readonly hasError = computed(() => this.errorKey() !== null || this.errorDetail() !== null);

  onCaptchaIdChange(id: string): void {
    this.form.controls.captchaId.setValue(id);
    this.form.controls.captchaId.markAsDirty();
    this.captchaReady.set(!!id);
  }

  onCaptchaAnswerChange(answer: string): void {
    this.form.controls.captchaAnswer.setValue(answer);
    this.form.controls.captchaAnswer.markAsDirty();
  }

  onCaptchaFailed(): void {
    this.form.controls.captchaId.setValue('');
    this.form.controls.captchaAnswer.setValue('');
    this.captchaReady.set(false);
  }

  canSubmit(): boolean {
    const { captchaId, captchaAnswer } = this.form.getRawValue();
    return (
      !this.submitting() &&
      this.captchaReady() &&
      this.form.valid &&
      !!captchaId &&
      !!captchaAnswer.trim()
    );
  }

  async submit(): Promise<void> {
    if (!this.canSubmit()) return;
    this.submitting.set(true);
    this.errorKey.set(null);
    this.errorDetail.set(null);
    try {
      const { username, password, captchaId, captchaAnswer } = this.form.getRawValue();
      const response = await this.auth.login({
        username,
        password,
        captchaId,
        captchaAnswer: captchaAnswer.trim(),
        rememberDevice: false
      });
      if (response.status === 'MFA_REQUIRED') {
        await this.router.navigate(['/auth/mfa']);
        return;
      }
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
      await this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : '/dashboard');
    } catch (err) {
      await this.captcha()?.refresh();
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
