import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

import { PasswordInputComponent } from '../password-input/password-input.component';

export interface PasswordConfirmDialogData {
  titleKey: string;
  messageKey: string;
  messageParams?: Record<string, unknown>;
  confirmKey?: string;
  cancelKey?: string;
  passwordLabelKey?: string;
  tone?: 'default' | 'danger';
}

@Component({
  selector: 'app-password-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    TranslatePipe,
    PasswordInputComponent
  ],
  template: `
    <div class="password-confirm" [attr.data-tone]="data.tone ?? 'default'">
      <div class="password-confirm__header">
        <mat-icon aria-hidden="true">lock</mat-icon>
        <h2 mat-dialog-title>{{ data.titleKey | translate }}</h2>
      </div>
      <div mat-dialog-content>
        <p>{{ data.messageKey | translate: data.messageParams }}</p>
        <form [formGroup]="form" (ngSubmit)="confirm()" id="password-confirm-form">
          <app-password-input
            formControlName="password"
            [label]="data.passwordLabelKey ?? 'common.passwordConfirm.passwordLabel'"
            autocomplete="current-password"
            name="confirmPassword"
          />
        </form>
      </div>
      <div mat-dialog-actions align="end">
        <button mat-button type="button" (click)="dialogRef.close(null)">
          {{ data.cancelKey ?? 'common.actions.cancel' | translate }}
        </button>
        <button
          mat-flat-button
          type="submit"
          form="password-confirm-form"
          [color]="data.tone === 'danger' ? 'warn' : 'primary'"
          [disabled]="form.invalid"
          cdkFocusInitial
        >
          {{ data.confirmKey ?? 'common.actions.confirm' | translate }}
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .password-confirm {
        min-inline-size: min(100%, 360px);
      }
      .password-confirm__header {
        display: flex;
        align-items: center;
        gap: var(--app-space-2);
        padding: var(--app-space-4) var(--app-space-5) 0;
      }
      .password-confirm__header h2 {
        margin: 0;
        font-size: var(--app-font-size-xl);
      }
      .password-confirm[data-tone='danger'] .password-confirm__header mat-icon {
        color: var(--app-color-danger);
      }
      .password-confirm p {
        margin-block: 0 var(--app-space-4);
        color: var(--app-color-text-secondary);
      }
    `
  ]
})
export class PasswordConfirmDialogComponent {
  readonly dialogRef =
    inject<MatDialogRef<PasswordConfirmDialogComponent, string | null>>(MatDialogRef);
  readonly data = inject<PasswordConfirmDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(1)]]
  });

  confirm(): void {
    if (this.form.invalid) return;
    this.dialogRef.close(this.form.controls.password.value);
  }
}

export async function openPasswordConfirmDialog(
  dialog: MatDialog,
  data: PasswordConfirmDialogData
): Promise<string | null> {
  const ref = dialog.open<PasswordConfirmDialogComponent, PasswordConfirmDialogData, string | null>(
    PasswordConfirmDialogComponent,
    { data, width: '460px', disableClose: true }
  );
  return (await firstValueFrom(ref.afterClosed())) ?? null;
}
