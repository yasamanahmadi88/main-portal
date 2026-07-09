import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule, TranslatePipe],
  template: `
    <div class="empty-state" role="status">
      <mat-icon class="empty-state__icon" aria-hidden="true">{{ icon() }}</mat-icon>
      <h2>{{ title() | translate }}</h2>
      @if (description()) {
        <p>{{ description()! | translate }}</p>
      }
      <ng-content></ng-content>
    </div>
  `,
  styles: [
    `
      .empty-state {
        text-align: center;
        padding: var(--app-space-10) var(--app-space-4);
        border-radius: var(--app-radius-lg);
        border: 1px dashed var(--app-color-outline);
        background-color: var(--app-color-surface-sunken);
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--app-space-2);
      }
      .empty-state__icon {
        font-size: 44px;
        width: 44px;
        height: 44px;
        color: var(--app-color-text-muted);
      }
      .empty-state h2 {
        margin: var(--app-space-2) 0 0;
        font-size: var(--app-font-size-xl);
      }
      .empty-state p {
        margin: 0;
        color: var(--app-color-text-secondary);
        max-width: 44ch;
      }
    `
  ]
})
export class EmptyStateComponent {
  readonly icon = input<string>('inbox');
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
}
