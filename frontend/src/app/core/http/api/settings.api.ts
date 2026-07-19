import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import type { Settings, SettingsPatchRequest } from '@shared/models';

@Injectable({ providedIn: 'root' })
export class SettingsApi {
  private readonly http = inject(HttpClient);

  get(): Observable<Settings> {
    return this.http.get<Settings>('/api/v1/settings', { withCredentials: true });
  }

  update(payload: SettingsPatchRequest): Observable<Settings> {
    return this.http.patch<Settings>('/api/v1/settings', payload, { withCredentials: true });
  }
}
