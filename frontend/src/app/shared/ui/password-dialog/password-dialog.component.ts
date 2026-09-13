import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { map } from 'rxjs/operators';

export interface PasswordDialogData {
  titleKey: string;
  messageKey?: string;
  confirmKey?: string;
  cancelKey?: string;
  icon?: string;
}

@Component({
  selector: 'app-password-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TranslatePipe
  ],
  template: `
    <div class="password-dialog">
      <div class="password-dialog__header">
        <mat-icon aria-hidden="true">{{ data.icon ?? 'lock' }}</mat-icon>
        <h2 mat-dialog-title>{{ data.titleKey | translate }}</h2>
      </div>
      @if (data.messageKey) {
        <div mat-dialog-content>
          <p>{{ data.messageKey | translate }}</p>
        </div>
      }
      <form mat-dialog-content [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>{{ 'profile.security.currentPasswordLabel' | translate }}</mat-label>
          <input
            matInput
            type="password"
            formControlName="password"
            autocomplete="current-password"
            cdkFocusInitial
          />
          @if (form.get('password')?.hasError('required') && form.get('password')?.touched) {
            <mat-error>{{ 'validation.required' | translate }}</mat-error>
          }
        </mat-form-field>
      </form>
      <div mat-dialog-actions align="end">
        <button mat-button type="button" (click)="dialogRef.close(null)">
          {{ data.cancelKey ?? 'common.actions.cancel' | translate }}
        </button>
        <button
          mat-flat-button
          type="button"
          color="primary"
          (click)="submit()"
          [disabled]="form.invalid"
        >
          {{ data.confirmKey ?? 'common.actions.confirm' | translate }}
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .password-dialog {
        min-width: 360px;
      }
      .password-dialog__header {
        display: flex;
        align-items: center;
        gap: var(--app-space-2);
        padding: var(--app-space-4) var(--app-space-5) 0;
      }
      .password-dialog__header h2 {
        margin: 0;
        font-size: var(--app-font-size-xl);
      }
      .full-width {
        width: 100%;
      }
    `
  ]
})
export class PasswordDialogComponent {
  readonly dialogRef = inject<MatDialogRef<PasswordDialogComponent, string | null>>(MatDialogRef);
  readonly data = inject<PasswordDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    password: ['', [Validators.required]]
  });

  submit(): void {
    if (this.form.invalid) return;
    this.dialogRef.close(this.form.getRawValue().password);
  }
}

export async function openPasswordDialog(
  dialog: MatDialog,
  data: PasswordDialogData
): Promise<string | null> {
  const ref = dialog.open<PasswordDialogComponent, PasswordDialogData, string | null>(
    PasswordDialogComponent,
    { data, width: '460px' }
  );
  return await firstValueFrom(ref.afterClosed().pipe(
    map(result => result ?? null)
  ));
}
