import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '@core/authentication/auth.service';
import { MeApi } from '@core/http/api/me.api';
import { I18nService } from '@core/i18n/i18n.service';
import { ThemeService } from '@core/i18n/theme.service';
import { ToastService } from '@core/observability/toast.service';
import type { Language, Theme } from '@shared/models';

@Component({
  selector: 'app-profile-overview-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    TranslatePipe
  ],
  templateUrl: './profile-overview-page.component.html',
  styleUrl: './profile-overview-page.component.scss'
})
export class ProfileOverviewPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly me = inject(MeApi);
  private readonly auth = inject(AuthService);
  private readonly i18n = inject(I18nService);
  private readonly theme = inject(ThemeService);
  private readonly toast = inject(ToastService);

  readonly submitting = signal(false);
  readonly user = this.auth.user;

  readonly form = this.fb.nonNullable.group({
    displayName: ['', [Validators.required, Validators.maxLength(160)]],
    email: ['', [Validators.required, Validators.email]],
    language: this.fb.nonNullable.control<Language>('fa-IR'),
    theme: this.fb.nonNullable.control<Theme>('SYSTEM')
  });

  constructor() {
    effect(() => {
      const user = this.user();
      if (user) {
        this.form.patchValue({
          displayName: user.displayName,
          email: user.email,
          language: user.preferences.language,
          theme: user.preferences.theme
        }, { emitEvent: false });
      }
    });
  }

  async save(): Promise<void> {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    const { displayName, email, language, theme } = this.form.getRawValue();
    try {
      const updated = await firstValueFrom(this.me.update({ displayName, email }));
      this.auth.setUser(updated);
      await this.i18n.use(language, { persist: false });
      this.theme.setMode(theme, { persist: false });
      await firstValueFrom(this.me.updatePreferences({ language, theme }));
      this.toast.success('profile.overview.saveSuccess');
    } finally {
      this.submitting.set(false);
    }
  }
}
