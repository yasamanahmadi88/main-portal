import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { toHttpParams } from '@core/http/http-params';
import type {
  ActionResult,
  AssignRolesRequest,
  CreateUserRequest,
  PageQuery,
  PageResponse,
  Role,
  UpdateUserRequest,
  User,
  UserActionRequest,
  UUID
} from '@shared/models';

export interface UserListQuery extends PageQuery {
  search?: string;
  status?: string;
  role?: string;
}

@Injectable({ providedIn: 'root' })
export class UsersApi {
  private readonly http = inject(HttpClient);

  list(query: UserListQuery = {}): Observable<PageResponse<User>> {
    return this.http.get<PageResponse<User>>('/api/v1/users', {
      params: toHttpParams(query),
      withCredentials: true
    });
  }

  get(id: UUID): Observable<User> {
    return this.http.get<User>(`/api/v1/users/${id}`, { withCredentials: true });
  }

  create(payload: CreateUserRequest): Observable<User> {
    return this.http.post<User>('/api/v1/users', payload, { withCredentials: true });
  }

  update(id: UUID, payload: UpdateUserRequest): Observable<User> {
    return this.http.patch<User>(`/api/v1/users/${id}`, payload, { withCredentials: true });
  }

  activate(id: UUID, payload: UserActionRequest = {}): Observable<ActionResult> {
    return this.http.post<ActionResult>(`/api/v1/users/${id}/activate`, payload, {
      withCredentials: true
    });
  }

  deactivate(id: UUID, payload: UserActionRequest = {}): Observable<ActionResult> {
    return this.http.post<ActionResult>(`/api/v1/users/${id}/deactivate`, payload, {
      withCredentials: true
    });
  }

  unlock(id: UUID, payload: UserActionRequest = {}): Observable<ActionResult> {
    return this.http.post<ActionResult>(`/api/v1/users/${id}/unlock`, payload, {
      withCredentials: true
    });
  }

  triggerPasswordReset(id: UUID, payload: UserActionRequest = {}): Observable<ActionResult> {
    return this.http.post<ActionResult>(`/api/v1/users/${id}/password-reset`, payload, {
      withCredentials: true
    });
  }

  resetMfa(id: UUID, payload: UserActionRequest = {}): Observable<ActionResult> {
    return this.http.post<ActionResult>(`/api/v1/users/${id}/mfa-reset`, payload, {
      withCredentials: true
    });
  }

  revokeSessions(id: UUID, payload: UserActionRequest = {}): Observable<ActionResult> {
    return this.http.post<ActionResult>(`/api/v1/users/${id}/sessions/revoke`, payload, {
      withCredentials: true
    });
  }

  getRoles(id: UUID): Observable<Role[]> {
    return this.http.get<Role[]>(`/api/v1/users/${id}/roles`, { withCredentials: true });
  }

  replaceRoles(id: UUID, payload: AssignRolesRequest): Observable<Role[]> {
    return this.http.put<Role[]>(`/api/v1/users/${id}/roles`, payload, {
      withCredentials: true
    });
  }
}
