import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { toHttpParams } from '@core/http/http-params';
import type {
  AcknowledgeSecurityEventRequest,
  PageQuery,
  PageResponse,
  SecurityEvent,
  UUID
} from '@shared/models';

export interface SecurityEventListQuery extends PageQuery {
  severity?: string;
  acknowledged?: boolean;
  from?: string;
  to?: string;
}

@Injectable({ providedIn: 'root' })
export class SecurityEventsApi {
  private readonly http = inject(HttpClient);

  list(query: SecurityEventListQuery = {}): Observable<PageResponse<SecurityEvent>> {
    return this.http.get<PageResponse<SecurityEvent>>('/api/v1/security-events', {
      params: toHttpParams(query),
      withCredentials: true
    });
  }

  acknowledge(id: UUID, payload: AcknowledgeSecurityEventRequest = {}): Observable<SecurityEvent> {
    return this.http.post<SecurityEvent>(
      `/api/v1/security-events/${id}/acknowledge`,
      payload,
      { withCredentials: true }
    );
  }
}
