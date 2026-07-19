import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';

import { MeApi } from '@core/http/api/me.api';
import { ToastService } from '@core/observability/toast.service';
import {
  PasswordInputComponent,
  StatusBadgeComponent,
  openConfirmDialog
} from '@shared/ui';
import type {
  MfaEnrollmentResult,
  MfaFactor,
  MfaSetup,
  RecoveryCodeStatus
} from '@shared/models';

function match(other: string) {
  return (control: AbstractControl): ValidationErrors | null => {
    const otherControl = control.parent?.get(other);
    if (!otherControl) return null;
    return otherControl.value === control.value ? null : { mismatch: true };
  };
}

@Component({
  selector: 'app-profile-security-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TranslatePipe,
    PasswordInputComponent,
    StatusBadgeComponent
  ],
  templateUrl: './profile-security-page.component.html',
  styleUrl: './profile-security-page.component.scss'
})
export class ProfileSecurityPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly me = inject(MeApi);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  readonly changingPassword = signal(false);
  readonly enrollingMfa = signal(false);
  readonly confirmingMfa = signal(false);
  readonly factors = signal<MfaFactor[]>([]);
  readonly recoveryStatus = signal<RecoveryCodeStatus | null>(null);
  readonly setup = signal<MfaSetup | null>(null);
  readonly newRecoveryCodes = signal<string[] | null>(null);

  readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(12)]],
    confirmPassword: ['', [Validators.required, match('newPassword')]]
  });

  readonly confirmForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(8)]]
  });

  constructor() {
    void this.refresh();
  }

  async refresh(): Promise<void> {
    const [factors, status] = await Promise.all([
      firstValueFrom(this.me.listMfa()).catch(() => [] as MfaFactor[]),
      firstValueFrom(this.me.getRecoveryCodeStatus()).catch(() => null)
    ]);
    this.factors.set(factors);
    this.recoveryStatus.set(status);
  }

  async changePassword(): Promise<void> {
    if (this.passwordForm.invalid || this.changingPassword()) return;
    this.changingPassword.set(true);
    try {
      const { currentPassword, newPassword } = this.passwordForm.getRawValue();
      await firstValueFrom(this.me.changePassword({ currentPassword, newPassword }));
      this.toast.success('profile.security.changePasswordSuccess');
      this.passwordForm.reset();
    } finally {
      this.changingPassword.set(false);
    }
  }

  async startTotpEnrollment(): Promise<void> {
    this.enrollingMfa.set(true);
    try {
      const setup = await firstValueFrom(this.me.setupTotp('Authenticator'));
      this.setup.set(setup);
    } finally {
      this.enrollingMfa.set(false);
    }
  }

  async confirmTotpEnrollment(): Promise<void> {
    const setup = this.setup();
    if (!setup || this.confirmForm.invalid || this.confirmingMfa()) return;
    this.confirmingMfa.set(true);
    try {
      const result: MfaEnrollmentResult = await firstValueFrom(
        this.me.confirmTotp({
          factorId: setup.factorId,
          code: this.confirmForm.controls.code.value
        })
      );
      this.newRecoveryCodes.set(result.recoveryCodes);
      this.setup.set(null);
      this.confirmForm.reset();
      this.toast.success('profile.security.enrollmentSuccess');
      await this.refresh();
    } finally {
      this.confirmingMfa.set(false);
    }
  }

  async removeFactor(factor: MfaFactor): Promise<void> {
    const ok = await openConfirmDialog(this.dialog, {
      titleKey: 'profile.security.confirmDeleteFactor',
      messageKey: 'profile.security.confirmDeleteFactorDetail',
      tone: 'danger'
    });
    if (!ok) return;
    const password = prompt('Confirm your password');
    if (!password) return;
    await firstValueFrom(this.me.deleteMfaFactor(factor.id, { password }));
    await this.refresh();
  }

  async regenerateRecoveryCodes(): Promise<void> {
    const password = prompt('Confirm your password');
    if (!password) return;
    const result = await firstValueFrom(this.me.regenerateRecoveryCodes({ password }));
    this.newRecoveryCodes.set(result.recoveryCodes);
    this.toast.success('profile.security.regenerateSuccess');
    await this.refresh();
  }

  copyCodes(): void {
    const codes = this.newRecoveryCodes();
    if (!codes || typeof navigator === 'undefined' || !navigator.clipboard) return;
    navigator.clipboard.writeText(codes.join('\n')).then(
      () => this.toast.success('common.actions.copied'),
      () => this.toast.warning('common.errors.title')
    );
  }

  dismissCodes(): void {
    this.newRecoveryCodes.set(null);
  }
}
