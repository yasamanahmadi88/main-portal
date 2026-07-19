import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';

import { SettingsApi } from '@core/http/api/settings.api';
import { ToastService } from '@core/observability/toast.service';
import {
  ErrorStateComponent,
  PageHeaderComponent,
  SkeletonComponent
} from '@shared/ui';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    TranslatePipe,
    PageHeaderComponent,
    ErrorStateComponent,
    SkeletonComponent
  ],
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss'
})
export class SettingsPageComponent {
  private readonly api = inject(SettingsApi);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly submitting = signal(false);

  readonly form = this.fb.nonNullable.group({
    defaultLanguage: this.fb.nonNullable.control<'fa-IR' | 'en-US'>('fa-IR'),
    defaultTheme: this.fb.nonNullable.control<'LIGHT' | 'DARK' | 'SYSTEM'>('SYSTEM'),
    sessionTimeoutMinutes: [30, [Validators.required, Validators.min(5), Validators.max(1440)]],
    mfaRequiredForAdmins: [true],
    auditRetentionDays: [365, [Validators.required, Validators.min(30)]],
    loginRateLimitPerMinute: [30, [Validators.min(1)]]
  });

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(false);
    try {
      const settings = await firstValueFrom(this.api.get());
      this.form.patchValue({
        defaultLanguage: settings.defaultLanguage,
        defaultTheme: settings.defaultTheme,
        sessionTimeoutMinutes: settings.sessionTimeoutMinutes,
        mfaRequiredForAdmins: settings.mfaRequiredForAdmins,
        auditRetentionDays: settings.auditRetentionDays,
        loginRateLimitPerMinute: settings.loginRateLimitPerMinute ?? 30
      });
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    try {
      await firstValueFrom(this.api.update(this.form.getRawValue()));
      this.toast.success('settings.actions.saved');
    } finally {
      this.submitting.set(false);
    }
  }
}
