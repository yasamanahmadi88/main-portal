import { HttpErrorResponse } from '@angular/common/http';

import type { ProblemDetail } from '@shared/models';

export interface NormalizedError {
  status: number;
  code?: string;
  title?: string;
  message: string;
  traceId?: string;
  fieldErrors: Record<string, string>;
  original: HttpErrorResponse | Error;
}

const CODE_MESSAGE_KEYS: Record<string, string> = {
  AUTH_INVALID_CREDENTIALS: 'authentication.errors.invalidCredentials',
  AUTH_ACCOUNT_LOCKED: 'authentication.errors.accountLocked',
  AUTH_MFA_REQUIRED: 'authentication.errors.mfaRequired',
  AUTH_MFA_INVALID: 'authentication.errors.mfaInvalid',
  AUTH_MFA_EXPIRED: 'authentication.errors.mfaExpired',
  AUTH_SESSION_EXPIRED: 'authentication.errors.sessionExpired',
  AUTH_FORBIDDEN: 'authentication.errors.forbidden',
  RATE_LIMITED: 'common.errors.rateLimited',
  rate_limited: 'common.errors.rateLimited',
  captcha_invalid: 'authentication.errors.captchaInvalid',
  captcha_expired: 'authentication.errors.captchaExpired',
  captcha_required: 'authentication.errors.captchaRequired',
  VALIDATION_FAILED: 'validation.errors.generic',
  validation_failed: 'validation.errors.generic',
  CONFLICT: 'common.errors.conflict',
  conflict: 'common.errors.conflict'
};

export function normalizeHttpError(err: unknown): NormalizedError {
  if (err instanceof HttpErrorResponse) {
    const problem = extractProblem(err);
    return {
      status: err.status,
      code: problem?.code,
      title: problem?.title,
      message: problem?.detail || problem?.title || err.message || 'Unexpected error',
      traceId: problem?.traceId,
      fieldErrors: mapFieldErrors(problem),
      original: err
    };
  }
  const error = err instanceof Error ? err : new Error(String(err));
  return {
    status: 0,
    message: error.message,
    fieldErrors: {},
    original: error
  };
}

export function messageKeyForError(err: NormalizedError): string | null {
  return err.code ? (CODE_MESSAGE_KEYS[err.code] ?? null) : null;
}

function extractProblem(err: HttpErrorResponse): ProblemDetail | null {
  const body = err.error as unknown;
  if (!body || typeof body !== 'object') {
    return null;
  }
  return body as ProblemDetail;
}

function mapFieldErrors(problem: ProblemDetail | null): Record<string, string> {
  if (!problem?.errors) {
    return {};
  }
  const map: Record<string, string> = {};
  for (const entry of problem.errors) {
    if (entry.field && !map[entry.field]) {
      map[entry.field] = entry.message;
    }
  }
  return map;
}
