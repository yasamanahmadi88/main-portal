import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="skeleton" [style.height]="height()" [style.width]="width()" aria-hidden="true"></span>`,
  styles: [
    `
      :host { display: inline-block; inline-size: 100%; }
      .skeleton {
        display: inline-block;
        border-radius: var(--app-radius-sm);
        background: linear-gradient(
          90deg,
          var(--app-color-surface-sunken) 0%,
          var(--app-color-outline) 50%,
          var(--app-color-surface-sunken) 100%
        );
        background-size: 200% 100%;
        animation: skeleton-shimmer 1.4s infinite;
        inline-size: 100%;
        block-size: 1rem;
      }
      @keyframes skeleton-shimmer {
        0% { background-position: 200% 0; }
        100% { background-position: -200% 0; }
      }
      @media (prefers-reduced-motion: reduce) {
        .skeleton { animation: none; opacity: 0.85; }
      }
    `
  ]
})
export class SkeletonComponent {
  readonly height = input<string>('1rem');
  readonly width = input<string>('100%');
}
