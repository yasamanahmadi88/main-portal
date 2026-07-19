import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-session-expired-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatIconModule, TranslatePipe],
  templateUrl: './session-expired-page.component.html',
  styleUrl: './session-expired-page.component.scss'
})
export class SessionExpiredPageComponent {
  private readonly route = inject(ActivatedRoute);
  readonly returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
}
