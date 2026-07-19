import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import type {
  MonitoringLogsSummary,
  MonitoringMetricsSummary,
  MonitoringOverview,
  MonitoringTracesSummary
} from '@shared/models';

@Injectable({ providedIn: 'root' })
export class MonitoringApi {
  private readonly http = inject(HttpClient);

  overview(): Observable<MonitoringOverview> {
    return this.http.get<MonitoringOverview>('/api/v1/monitoring/overview', {
      withCredentials: true
    });
  }

  metrics(): Observable<MonitoringMetricsSummary> {
    return this.http.get<MonitoringMetricsSummary>('/api/v1/monitoring/metrics', {
      withCredentials: true
    });
  }

  logs(): Observable<MonitoringLogsSummary> {
    return this.http.get<MonitoringLogsSummary>('/api/v1/monitoring/logs', {
      withCredentials: true
    });
  }

  traces(): Observable<MonitoringTracesSummary> {
    return this.http.get<MonitoringTracesSummary>('/api/v1/monitoring/traces', {
      withCredentials: true
    });
  }
}
