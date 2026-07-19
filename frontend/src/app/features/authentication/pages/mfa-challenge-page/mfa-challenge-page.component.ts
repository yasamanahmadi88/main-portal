import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '@core/authentication/auth.service';
import { messageKeyForError, normalizeHttpError } from '@core/error-handling/problem-details';

@Component({
  selector: 'app-mfa-challenge-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    TranslatePipe
  ],
  templateUrl: './mfa-challenge-page.component.html',
  styleUrl: './mfa-challenge-page.component.scss'
})
export class MfaChallengePageComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly mode = signal<'TOTP' | 'RECOVERY_CODE'>('TOTP');
  readonly submitting = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly challenge = this.auth.pendingChallenge;

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(8)]]
  });
  readonly recoveryForm = this.fb.nonNullable.group({
    recoveryCode: ['', [Validators.required, Validators.minLength(6)]]
  });

  readonly currentForm = computed(() =>
    this.mode() === 'TOTP' ? this.form : this.recoveryForm
  );

  useRecovery(): void {
    this.mode.set('RECOVERY_CODE');
    this.errorKey.set(null);
  }

  useAuthenticator(): void {
    this.mode.set('TOTP');
    this.errorKey.set(null);
  }

  async submit(): Promise<void> {
    const challenge = this.challenge();
    if (!challenge) {
      await this.router.navigate(['/auth/login']);
      return;
    }
    const active = this.currentForm();
    if (active.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.errorKey.set(null);
    try {
      if (this.mode() === 'TOTP') {
        await this.auth.verifyMfa({
          challengeId: challenge.challengeId,
          code: this.form.controls.code.value
        });
      } else {
        await this.auth.submitRecoveryCode({
          challengeId: challenge.challengeId,
          recoveryCode: this.recoveryForm.controls.recoveryCode.value
        });
      }
      await this.router.navigateByUrl('/dashboard');
    } catch (err) {
      if (err instanceof HttpErrorResponse) {
        const normalized = normalizeHttpError(err);
        const key = messageKeyForError(normalized);
        this.errorKey.set(key ?? 'authentication.errors.mfaInvalid');
      } else {
        this.errorKey.set('common.errors.server');
      }
    } finally {
      this.submitting.set(false);
    }
  }
}
