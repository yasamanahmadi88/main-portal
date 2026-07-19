import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

export interface BreadcrumbItem {
  readonly labelKey: string;
  readonly link?: string;
}

@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatIconModule, TranslatePipe],
  template: `
    <nav class="breadcrumb" aria-label="breadcrumb">
      <ol>
        @for (item of items(); track item.labelKey; let last = $last) {
          <li>
            @if (item.link && !last) {
              <a [routerLink]="item.link">{{ item.labelKey | translate }}</a>
            } @else {
              <span [attr.aria-current]="last ? 'page' : null">{{ item.labelKey | translate }}</span>
            }
            @if (!last) {
              <mat-icon class="breadcrumb__sep" aria-hidden="true">chevron_right</mat-icon>
            }
          </li>
        }
      </ol>
    </nav>
  `,
  styles: [
    `
      .breadcrumb ol {
        display: flex;
        gap: var(--app-space-1);
        list-style: none;
        padding: 0;
        margin: 0;
        color: var(--app-color-text-secondary);
        flex-wrap: wrap;
      }
      .breadcrumb li {
        display: inline-flex;
        align-items: center;
        gap: var(--app-space-1);
        font-size: var(--app-font-size-sm);
      }
      .breadcrumb a {
        color: var(--app-color-text-secondary);
      }
      .breadcrumb a:hover {
        color: var(--app-color-link-hover);
      }
      .breadcrumb__sep {
        font-size: 16px;
        width: 16px;
        height: 16px;
        color: var(--app-color-text-muted);
      }
      :host-context(html[dir='rtl']) .breadcrumb__sep {
        transform: scaleX(-1);
      }
    `
  ]
})
export class BreadcrumbComponent {
  readonly items = input.required<readonly BreadcrumbItem[]>();
}
