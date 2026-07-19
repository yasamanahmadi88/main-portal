import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { toHttpParams } from '@core/http/http-params';
import type { PageQuery, PageResponse, Session } from '@shared/models';

export interface SessionListQuery extends PageQuery {
  userId?: string;
  active?: boolean;
}

@Injectable({ providedIn: 'root' })
export class SessionsApi {
  private readonly http = inject(HttpClient);

  list(query: SessionListQuery = {}): Observable<PageResponse<Session>> {
    return this.http.get<PageResponse<Session>>('/api/v1/sessions', {
      params: toHttpParams(query),
      withCredentials: true
    });
  }

  revoke(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/sessions/${id}`, { withCredentials: true });
  }
}
