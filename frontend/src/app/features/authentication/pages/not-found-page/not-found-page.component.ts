import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-not-found-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatIconModule, TranslatePipe],
  template: `
    <section class="auth-card">
      <header>
        <mat-icon aria-hidden="true" class="auth-card__icon">explore_off</mat-icon>
        <h1>{{ 'common.errors.notFoundTitle' | translate }}</h1>
        <p>{{ 'common.errors.notFoundDescription' | translate }}</p>
      </header>
      <a mat-flat-button color="primary" class="auth-card__submit" [routerLink]="'/dashboard'">
        {{ 'authentication.accessDenied.backHome' | translate }}
      </a>
    </section>
  `,
  styleUrl: './not-found-page.component.scss'
})
export class NotFoundPageComponent {}
