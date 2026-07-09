import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
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

export interface ConfirmDialogData {
  titleKey: string;
  messageKey: string;
  messageParams?: Record<string, unknown>;
  confirmKey?: string;
  cancelKey?: string;
  tone?: 'default' | 'danger';
  icon?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatIconModule, TranslatePipe],
  template: `
    <div class="confirm-dialog" [attr.data-tone]="data.tone ?? 'default'">
      <div class="confirm-dialog__header">
        <mat-icon aria-hidden="true">{{ data.icon ?? 'help_outline' }}</mat-icon>
        <h2 mat-dialog-title>{{ data.titleKey | translate }}</h2>
      </div>
      <div mat-dialog-content>
        <p>{{ data.messageKey | translate: data.messageParams }}</p>
      </div>
      <div mat-dialog-actions align="end">
        <button mat-button type="button" (click)="dialogRef.close(false)">
          {{ data.cancelKey ?? 'common.actions.cancel' | translate }}
        </button>
        <button
          mat-flat-button
          type="button"
          [color]="data.tone === 'danger' ? 'warn' : 'primary'"
          (click)="dialogRef.close(true)"
          cdkFocusInitial
        >
          {{ data.confirmKey ?? 'common.actions.confirm' | translate }}
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .confirm-dialog {
        min-width: 320px;
      }
      .confirm-dialog__header {
        display: flex;
        align-items: center;
        gap: var(--app-space-2);
        padding: var(--app-space-4) var(--app-space-5) 0;
      }
      .confirm-dialog__header h2 {
        margin: 0;
        font-size: var(--app-font-size-xl);
      }
      .confirm-dialog[data-tone='danger'] .confirm-dialog__header mat-icon {
        color: var(--app-color-danger);
      }
    `
  ]
})
export class ConfirmDialogComponent {
  readonly dialogRef =
    inject<MatDialogRef<ConfirmDialogComponent, boolean>>(MatDialogRef);
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}

export async function openConfirmDialog(
  dialog: MatDialog,
  data: ConfirmDialogData
): Promise<boolean> {
  const ref = dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(
    ConfirmDialogComponent,
    { data, width: '460px' }
  );
  return (await firstValueFrom(ref.afterClosed())) === true;
}
