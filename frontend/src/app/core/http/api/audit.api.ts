import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { toHttpParams } from '@core/http/http-params';
import type {
  AuditEvent,
  AuditExportRequest,
  ExportJob,
  PageQuery,
  PageResponse,
  UUID
} from '@shared/models';

export interface AuditListQuery extends PageQuery {
  action?: string;
  actorId?: string;
  from?: string;
  to?: string;
  outcome?: string;
}

@Injectable({ providedIn: 'root' })
export class AuditApi {
  private readonly http = inject(HttpClient);

  list(query: AuditListQuery = {}): Observable<PageResponse<AuditEvent>> {
    return this.http.get<PageResponse<AuditEvent>>('/api/v1/audit-events', {
      params: toHttpParams(query),
      withCredentials: true
    });
  }

  get(id: UUID): Observable<AuditEvent> {
    return this.http.get<AuditEvent>(`/api/v1/audit-events/${id}`, { withCredentials: true });
  }

  export(payload: AuditExportRequest): Observable<ExportJob> {
    return this.http.post<ExportJob>('/api/v1/audit-events/export', payload, {
      withCredentials: true
    });
  }

  verifyIntegrity(payload: { from?: string; to?: string } = {}): Observable<{
    valid: boolean;
    checkedEvents: number;
    firstInvalidEventId?: string | null;
    verifiedAt: string;
  }> {
    return this.http.post<{
      valid: boolean;
      checkedEvents: number;
      firstInvalidEventId?: string | null;
      verifiedAt: string;
    }>('/api/v1/audit-events/verify-integrity', payload, { withCredentials: true });
  }
}
