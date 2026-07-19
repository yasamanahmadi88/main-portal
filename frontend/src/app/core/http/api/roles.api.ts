import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { toHttpParams } from '@core/http/http-params';
import type {
  AssignPermissionsRequest,
  PageQuery,
  PageResponse,
  Permission,
  PermissionMatrix,
  Role,
  RolePatchRequest,
  RoleWriteRequest,
  UUID
} from '@shared/models';

export interface RoleListQuery extends PageQuery {
  search?: string;
}

@Injectable({ providedIn: 'root' })
export class RolesApi {
  private readonly http = inject(HttpClient);

  list(query: RoleListQuery = {}): Observable<PageResponse<Role>> {
    return this.http.get<PageResponse<Role>>('/api/v1/roles', {
      params: toHttpParams(query),
      withCredentials: true
    });
  }

  get(id: UUID): Observable<Role> {
    return this.http.get<Role>(`/api/v1/roles/${id}`, { withCredentials: true });
  }

  create(payload: RoleWriteRequest): Observable<Role> {
    return this.http.post<Role>('/api/v1/roles', payload, { withCredentials: true });
  }

  update(id: UUID, payload: RolePatchRequest): Observable<Role> {
    return this.http.patch<Role>(`/api/v1/roles/${id}`, payload, { withCredentials: true });
  }

  delete(id: UUID): Observable<void> {
    return this.http.delete<void>(`/api/v1/roles/${id}`, { withCredentials: true });
  }

  getPermissions(id: UUID): Observable<Permission[]> {
    return this.http.get<Permission[]>(`/api/v1/roles/${id}/permissions`, {
      withCredentials: true
    });
  }

  replacePermissions(id: UUID, payload: AssignPermissionsRequest): Observable<Permission[]> {
    return this.http.put<Permission[]>(`/api/v1/roles/${id}/permissions`, payload, {
      withCredentials: true
    });
  }
}

@Injectable({ providedIn: 'root' })
export class PermissionsApi {
  private readonly http = inject(HttpClient);

  list(query: PageQuery = {}): Observable<PageResponse<Permission>> {
    return this.http.get<PageResponse<Permission>>('/api/v1/permissions', {
      params: toHttpParams(query),
      withCredentials: true
    });
  }

  getMatrix(): Observable<PermissionMatrix> {
    return this.http.get<PermissionMatrix>('/api/v1/permissions/matrix', {
      withCredentials: true
    });
  }
}
