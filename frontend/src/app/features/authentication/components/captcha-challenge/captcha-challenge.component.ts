import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  input,
  model,
  output,
  signal
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, type SafeHtml } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';

import type { CaptchaChallenge } from '@shared/models';

@Component({
  selector: 'app-captcha-challenge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    TranslatePipe
  ],
  templateUrl: './captcha-challenge.component.html',
  styleUrl: './captcha-challenge.component.scss'
})
export class CaptchaChallengeComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly sanitizer = inject(DomSanitizer);

  /** When true, loads a challenge immediately on init. */
  readonly autoload = input(true);

  readonly captchaId = model<string>('');
  readonly captchaAnswer = model<string>('');

  readonly ready = signal(false);
  readonly loading = signal(false);
  readonly loadError = signal(false);
  readonly imageHtml = signal<SafeHtml | null>(null);
  readonly revealAnswer = signal<string | null>(null);

  readonly loaded = output<CaptchaChallenge>();
  readonly failed = output<void>();

  ngOnInit(): void {
    if (this.autoload()) {
      void this.refresh();
    }
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    this.loadError.set(false);
    this.ready.set(false);
    this.captchaId.set('');
    this.captchaAnswer.set('');
    this.revealAnswer.set(null);
    try {
      const challenge = await firstValueFrom(
        this.http.get<CaptchaChallenge>('/api/v1/auth/captcha', { withCredentials: true })
      );
      this.captchaId.set(challenge.captchaId);
      this.imageHtml.set(this.sanitizer.bypassSecurityTrustHtml(challenge.imageSvg));
      this.revealAnswer.set(challenge.revealAnswer ?? null);
      this.ready.set(true);
      this.loaded.emit(challenge);
    } catch {
      this.captchaId.set('');
      this.imageHtml.set(null);
      this.loadError.set(true);
      this.failed.emit();
    } finally {
      this.loading.set(false);
    }
  }

  onAnswerInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.captchaAnswer.set(value);
  }
}
