import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-page-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  template: `
    <header class="app-page-header">
      <div class="app-page-header__text">
        <h1>{{ title() | translate }}</h1>
        @if (subtitle()) {
          <p>{{ subtitle()! | translate }}</p>
        }
      </div>
      @if (actionsVisible()) {
        <div class="app-page-header__actions">
          <ng-content select="[actions]"></ng-content>
        </div>
      }
    </header>
  `,
  styles: [
    `
      :host {
        display: block;
        margin-block-end: var(--app-space-6);
      }
      .app-page-header {
        display: flex;
        gap: var(--app-space-4);
        align-items: flex-start;
        justify-content: space-between;
        flex-wrap: wrap;
      }
      .app-page-header__text h1 {
        margin: 0 0 var(--app-space-1);
      }
      .app-page-header__text p {
        margin: 0;
        color: var(--app-color-text-secondary);
      }
      .app-page-header__actions {
        display: flex;
        gap: var(--app-space-2);
        flex-wrap: wrap;
      }
    `
  ]
})
export class PageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
  readonly actionsVisible = input<boolean>(true);
}
