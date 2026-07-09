// Data transfer objects mirroring the portal-v1 OpenAPI contract.
// Only the fields the frontend currently consumes are declared here; the
// contract is the source of truth and additional fields will surface as
// they're needed without breaking existing UI.

export type UUID = string;
export type Iso8601 = string;

export type Language = 'fa-IR' | 'en-US';
export type Theme = 'LIGHT' | 'DARK' | 'SYSTEM';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'PENDING';
export type SecurityEventSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type AuditOutcome = 'SUCCESS' | 'FAILURE';

export interface Preferences {
  language: Language;
  theme: Theme;
  timezone: string;
  density?: 'COMFORTABLE' | 'COMPACT';
}

export interface Permission {
  code: string;
  module: string;
  action: string;
  description: string;
  system?: boolean;
}

export interface Role {
  id: UUID;
  code: string;
  name: string;
  description?: string | null;
  system: boolean;
  permissions: Permission[];
  createdAt: Iso8601;
  updatedAt: Iso8601;
}

export interface User {
  id: UUID;
  username: string;
  email: string;
  displayName: string;
  status: UserStatus;
  preferences: Preferences;
  roles: Role[];
  permissions: string[];
  mfaEnabled: boolean;
  lastLoginAt?: Iso8601 | null;
  createdAt: Iso8601;
  updatedAt: Iso8601;
}

export interface Session {
  id: string;
  userId: UUID;
  username?: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt: Iso8601;
  lastAccessedAt: Iso8601;
  expiresAt: Iso8601;
  revokedAt?: Iso8601 | null;
  current: boolean;
}

export interface MfaFactor {
  id: UUID;
  type: 'TOTP';
  name: string;
  enabled: boolean;
  createdAt: Iso8601;
  lastUsedAt?: Iso8601 | null;
}

export interface MfaSetup {
  factorId: UUID;
  type: 'TOTP';
  secret: string;
  otpAuthUri: string;
  qrCodeSvg?: string;
  expiresAt: Iso8601;
}

export interface MfaEnrollmentResult {
  factor: MfaFactor;
  recoveryCodes: string[];
}

export interface RecoveryCodeStatus {
  remainingCodes: number;
  lastRegeneratedAt?: Iso8601 | null;
}

export interface RecoveryCodes {
  recoveryCodes: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
  rememberDevice?: boolean;
}

export interface MfaChallenge {
  challengeId: string;
  methods: ('TOTP' | 'RECOVERY_CODE')[];
  expiresAt: Iso8601;
}

export interface LoginResponse {
  status: 'AUTHENTICATED' | 'MFA_REQUIRED';
  user?: User;
  session?: Session;
  mfaChallenge?: MfaChallenge;
}

export interface MfaVerifyRequest {
  challengeId: string;
  code: string;
  rememberDevice?: boolean;
}

export interface MfaRecoveryRequest {
  challengeId: string;
  recoveryCode: string;
  rememberDevice?: boolean;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface CsrfToken {
  headerName: string;
  token: string;
}

export interface UpdateMeRequest {
  displayName?: string;
  email?: string;
}

export interface PreferencesPatchRequest {
  language?: Language;
  theme?: Theme;
  timezone?: string;
  density?: 'COMFORTABLE' | 'COMPACT';
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  displayName: string;
  roleIds?: UUID[];
  sendInvite?: boolean;
  preferences?: Preferences;
}

export interface UpdateUserRequest {
  email?: string;
  displayName?: string;
  status?: UserStatus;
  preferences?: PreferencesPatchRequest;
}

export interface UserActionRequest {
  reason?: string;
  notifyUser?: boolean;
}

export interface AssignRolesRequest {
  roleIds: UUID[];
}

export interface RoleWriteRequest {
  code: string;
  name: string;
  description?: string;
}

export interface RolePatchRequest {
  name?: string;
  description?: string | null;
}

export interface AssignPermissionsRequest {
  permissionCodes: string[];
}

export interface PermissionMatrix {
  roles: Role[];
  permissions: Permission[];
  assignments: { roleId: UUID; permissionCode: string }[];
}

export interface AuditActor {
  type: 'USER' | 'SYSTEM' | 'SERVICE';
  userId?: UUID | null;
  username?: string | null;
}

export interface AuditResource {
  type: string;
  id?: string | null;
  displayName?: string | null;
}

export interface AuditEvent {
  id: UUID;
  occurredAt: Iso8601;
  actor: AuditActor;
  action: string;
  resource: AuditResource;
  outcome: AuditOutcome;
  ipAddress?: string;
  userAgent?: string;
  metadata?: Record<string, unknown>;
  previousHash?: string | null;
  hash: string;
}

export interface SecurityEvent {
  id: UUID;
  occurredAt: Iso8601;
  type: string;
  severity: SecurityEventSeverity;
  userId?: UUID | null;
  ipAddress?: string;
  details?: Record<string, unknown>;
  acknowledged: boolean;
  acknowledgedBy?: UUID | null;
  acknowledgedAt?: Iso8601 | null;
  acknowledgementNote?: string | null;
}

export interface Settings {
  defaultLanguage: Language;
  defaultTheme: Theme;
  sessionTimeoutMinutes: number;
  mfaRequiredForAdmins: boolean;
  auditRetentionDays: number;
  loginRateLimitPerMinute?: number;
}

export interface SettingsPatchRequest extends Partial<Settings> {}

export interface DashboardSummary {
  activeUsers: number;
  activeSessions: number;
  securityEvents: {
    unacknowledged: number;
    critical: number;
  };
  auditEventsToday: number;
}

export interface PageResponse<T> {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  numberOfElements: number;
  first?: boolean;
  last?: boolean;
  content: T[];
}

export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string[];
  [k: string]: unknown;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  traceId?: string;
  errors?: { field: string; message: string; code?: string }[];
}

export interface ActionResult {
  message: string;
  id?: string;
  completedAt?: Iso8601;
}

export interface AcknowledgeSecurityEventRequest {
  note?: string;
}

export interface AuditExportRequest {
  format: 'CSV' | 'JSON';
  filters?: Record<string, unknown>;
}

export interface ExportJob {
  id: UUID;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  requestedAt: Iso8601;
}
