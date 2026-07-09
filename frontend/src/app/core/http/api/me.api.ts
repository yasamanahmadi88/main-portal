import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import type {
  ActionResult,
  ChangePasswordRequest,
  MfaEnrollmentResult,
  MfaFactor,
  MfaSetup,
  Preferences,
  PreferencesPatchRequest,
  RecoveryCodeStatus,
  RecoveryCodes,
  Session,
  UpdateMeRequest,
  User,
  UUID
} from '@shared/models';

export interface MfaConfirmRequestType {
  factorId: UUID;
  code: string;
}
export interface PasswordConfirmationRequestType {
  password: string;
}

@Injectable({ providedIn: 'root' })
export class MeApi {
  private readonly http = inject(HttpClient);

  get(): Observable<User> {
    return this.http.get<User>('/api/v1/me', { withCredentials: true });
  }

  update(payload: UpdateMeRequest): Observable<User> {
    return this.http.patch<User>('/api/v1/me', payload, { withCredentials: true });
  }

  getPreferences(): Observable<Preferences> {
    return this.http.get<Preferences>('/api/v1/me/preferences', { withCredentials: true });
  }

  updatePreferences(payload: PreferencesPatchRequest): Observable<Preferences> {
    return this.http.patch<Preferences>('/api/v1/me/preferences', payload, {
      withCredentials: true
    });
  }

  changePassword(payload: ChangePasswordRequest): Observable<ActionResult> {
    return this.http.post<ActionResult>('/api/v1/me/password', payload, {
      withCredentials: true
    });
  }

  listSessions(): Observable<Session[]> {
    return this.http.get<Session[]>('/api/v1/me/sessions', { withCredentials: true });
  }

  revokeSession(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/me/sessions/${id}`, { withCredentials: true });
  }

  revokeOtherSessions(
    payload: PasswordConfirmationRequestType
  ): Observable<{ revokedCount: number }> {
    return this.http.post<{ revokedCount: number }>(
      '/api/v1/me/sessions/revoke-others',
      payload,
      { withCredentials: true }
    );
  }

  listMfa(): Observable<MfaFactor[]> {
    return this.http.get<MfaFactor[]>('/api/v1/me/mfa', { withCredentials: true });
  }

  setupTotp(name: string): Observable<MfaSetup> {
    return this.http.post<MfaSetup>(
      '/api/v1/me/mfa/totp/setup',
      { name },
      { withCredentials: true }
    );
  }

  confirmTotp(payload: MfaConfirmRequestType): Observable<MfaEnrollmentResult> {
    return this.http.post<MfaEnrollmentResult>('/api/v1/me/mfa/totp/confirm', payload, {
      withCredentials: true
    });
  }

  deleteMfaFactor(factorId: UUID, payload: PasswordConfirmationRequestType): Observable<void> {
    return this.http.request<void>('DELETE', `/api/v1/me/mfa/${factorId}`, {
      body: payload,
      withCredentials: true
    });
  }

  getRecoveryCodeStatus(): Observable<RecoveryCodeStatus> {
    return this.http.get<RecoveryCodeStatus>('/api/v1/me/mfa/recovery-codes', {
      withCredentials: true
    });
  }

  regenerateRecoveryCodes(
    payload: PasswordConfirmationRequestType
  ): Observable<RecoveryCodes> {
    return this.http.post<RecoveryCodes>('/api/v1/me/mfa/recovery-codes/regenerate', payload, {
      withCredentials: true
    });
  }
}
