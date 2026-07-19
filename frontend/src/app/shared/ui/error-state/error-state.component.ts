import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-error-state',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, TranslatePipe],
  template: `
    <div class="error-state" role="alert">
      <mat-icon aria-hidden="true">error_outline</mat-icon>
      <div class="error-state__body">
        <h2>{{ title() | translate }}</h2>
        <p>{{ description() | translate }}</p>
      </div>
      @if (retryable()) {
        <button mat-stroked-button type="button" color="primary" (click)="retry.emit()">
          <mat-icon aria-hidden="true">refresh</mat-icon>
          <span>{{ 'common.actions.retry' | translate }}</span>
        </button>
      }
    </div>
  `,
  styles: [
    `
      .error-state {
        display: flex;
        gap: var(--app-space-4);
        align-items: center;
        padding: var(--app-space-5);
        border-radius: var(--app-radius-lg);
        background-color: var(--app-color-danger-soft);
        color: var(--app-color-text);
        border: 1px solid color-mix(in oklab, var(--app-color-danger) 30%, transparent);
      }
      .error-state mat-icon {
        color: var(--app-color-danger);
        font-size: 32px;
        width: 32px;
        height: 32px;
      }
      .error-state__body {
        flex: 1;
      }
      .error-state h2 {
        font-size: var(--app-font-size-lg);
        margin: 0 0 var(--app-space-1);
      }
      .error-state p {
        margin: 0;
        color: var(--app-color-text-secondary);
      }
    `
  ]
})
export class ErrorStateComponent {
  readonly title = input<string>('common.errors.title');
  readonly description = input<string>('common.errors.description');
  readonly retryable = input<boolean>(true);
  readonly retry = output<void>();
}
