import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-maintenance-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, TranslatePipe],
  template: `
    <section class="auth-card">
      <header>
        <mat-icon aria-hidden="true" class="auth-card__icon">construction</mat-icon>
        <h1>{{ 'authentication.maintenance.title' | translate }}</h1>
        <p>{{ 'authentication.maintenance.description' | translate }}</p>
      </header>
      <button mat-flat-button color="primary" class="auth-card__submit" (click)="reload()">
        {{ 'authentication.maintenance.retry' | translate }}
      </button>
    </section>
  `,
  styleUrl: './maintenance-page.component.scss'
})
export class MaintenancePageComponent {
  reload(): void {
    if (typeof location !== 'undefined') {
      location.reload();
    }
  }
}
