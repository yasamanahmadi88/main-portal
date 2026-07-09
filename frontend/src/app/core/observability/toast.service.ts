import { Injectable, inject } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';

type Tone = 'info' | 'success' | 'warning' | 'error';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  info(key: string, params?: Record<string, unknown>): void {
    this.show('info', key, params, 5000);
  }

  success(key: string, params?: Record<string, unknown>): void {
    this.show('success', key, params, 4000);
  }

  warning(key: string, params?: Record<string, unknown>): void {
    this.show('warning', key, params, 6000);
  }

  error(key: string, params?: Record<string, unknown>): void {
    this.show('error', key, params, 8000);
  }

  private show(
    tone: Tone,
    key: string,
    params: Record<string, unknown> | undefined,
    duration: number
  ): void {
    const label = this.translate.instant(key, params) || key;
    const dismiss = this.translate.instant('common.actions.dismiss');
    const config: MatSnackBarConfig = {
      duration,
      politeness: tone === 'error' ? 'assertive' : 'polite',
      panelClass: [`snackbar-${tone}`],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    };
    this.snackBar.open(label, dismiss || 'OK', config);
  }
}
