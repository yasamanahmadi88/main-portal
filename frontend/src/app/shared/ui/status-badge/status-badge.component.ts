import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

type Tone = 'neutral' | 'primary' | 'success' | 'warning' | 'danger' | 'info';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  template: `
    <span class="status-badge" [attr.data-tone]="tone()">
      @if (label()) {
        <span>{{ label() | translate }}</span>
      } @else {
        <ng-content></ng-content>
      }
    </span>
  `,
  styles: [
    `
      :host { display: inline-flex; }
      .status-badge {
        display: inline-flex;
        align-items: center;
        gap: var(--app-space-1);
        border-radius: var(--app-radius-pill);
        padding: 2px var(--app-space-3);
        font-size: var(--app-font-size-xs);
        font-weight: 600;
        line-height: 1.4;
        background-color: var(--app-color-surface-sunken);
        color: var(--app-color-text-secondary);
        border: 1px solid var(--app-color-outline);
        white-space: nowrap;
      }
      .status-badge[data-tone='primary'] {
        background-color: var(--app-color-primary-soft);
        color: var(--app-color-primary);
        border-color: transparent;
      }
      .status-badge[data-tone='success'] {
        background-color: var(--app-color-success-soft);
        color: var(--app-color-success);
        border-color: transparent;
      }
      .status-badge[data-tone='warning'] {
        background-color: var(--app-color-warning-soft);
        color: var(--app-color-warning);
        border-color: transparent;
      }
      .status-badge[data-tone='danger'] {
        background-color: var(--app-color-danger-soft);
        color: var(--app-color-danger);
        border-color: transparent;
      }
      .status-badge[data-tone='info'] {
        background-color: var(--app-color-info-soft);
        color: var(--app-color-info);
        border-color: transparent;
      }
    `
  ]
})
export class StatusBadgeComponent {
  readonly tone = input<Tone>('neutral');
  readonly label = input<string | null>(null);

  readonly _computedTone = computed(() => this.tone());
}
