import { Routes } from '@angular/router';

import { AppShellComponent } from '@core/shell/app-shell.component';
import { AuthLayoutComponent } from '@core/shell/auth-layout.component';
import {
  authGuard,
  guestGuard,
  mfaChallengeGuard,
  permissionGuard
} from '@core/authentication/guards';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard'
  },
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      {
        path: 'login',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('@features/authentication/pages/login-page/login-page.component').then(
            (m) => m.LoginPageComponent
          ),
        title: 'authentication.login.title'
      },
      {
        path: 'forgot-password',
        canActivate: [guestGuard],
        loadComponent: () =>
          import(
            '@features/authentication/pages/forgot-password-page/forgot-password-page.component'
          ).then((m) => m.ForgotPasswordPageComponent),
        title: 'authentication.forgotPassword.title'
      },
      {
        path: 'reset-password',
        canActivate: [guestGuard],
        loadComponent: () =>
          import(
            '@features/authentication/pages/reset-password-page/reset-password-page.component'
          ).then((m) => m.ResetPasswordPageComponent),
        title: 'authentication.resetPassword.title'
      },
      {
        path: 'mfa',
        canActivate: [mfaChallengeGuard],
        loadComponent: () =>
          import(
            '@features/authentication/pages/mfa-challenge-page/mfa-challenge-page.component'
          ).then((m) => m.MfaChallengePageComponent),
        title: 'authentication.mfa.title'
      },
      {
        path: 'session-expired',
        loadComponent: () =>
          import(
            '@features/authentication/pages/session-expired-page/session-expired-page.component'
          ).then((m) => m.SessionExpiredPageComponent),
        title: 'authentication.sessionExpired.title'
      },
      { path: '', pathMatch: 'full', redirectTo: 'login' }
    ]
  },
  {
    path: 'access-denied',
    component: AuthLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@features/authentication/pages/access-denied-page/access-denied-page.component').then(
            (m) => m.AccessDeniedPageComponent
          ),
        title: 'authentication.accessDenied.title'
      }
    ]
  },
  {
    path: 'maintenance',
    component: AuthLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@features/authentication/pages/maintenance-page/maintenance-page.component').then(
            (m) => m.MaintenancePageComponent
          ),
        title: 'authentication.maintenance.title'
      }
    ]
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('@features/dashboard/dashboard-page/dashboard-page.component').then(
            (m) => m.DashboardPageComponent
          ),
        title: 'dashboard.title'
      },
      {
        path: 'profile',
        loadChildren: () =>
          import('@features/profile/profile.routes').then((m) => m.PROFILE_ROUTES)
      },
      {
        path: 'users',
        canActivate: [permissionGuard],
        data: { permissions: ['USER_READ'] },
        loadChildren: () =>
          import('@features/users/users.routes').then((m) => m.USERS_ROUTES)
      },
      {
        path: 'roles',
        canActivate: [permissionGuard],
        data: { permissions: ['ROLE_READ'] },
        loadChildren: () =>
          import('@features/roles/roles.routes').then((m) => m.ROLES_ROUTES)
      },
      {
        path: 'permissions',
        canActivate: [permissionGuard],
        data: { permissions: ['PERMISSION_READ'] },
        loadComponent: () =>
          import('@features/permissions/permissions-page/permissions-page.component').then(
            (m) => m.PermissionsPageComponent
          ),
        title: 'permissions.title'
      },
      {
        path: 'audit',
        canActivate: [permissionGuard],
        data: { permissions: ['AUDIT_READ'] },
        loadChildren: () =>
          import('@features/audit/audit.routes').then((m) => m.AUDIT_ROUTES)
      },
      {
        path: 'security-events',
        canActivate: [permissionGuard],
        data: { permissions: ['SECURITY_READ'] },
        loadComponent: () =>
          import(
            '@features/security-events/security-events-page/security-events-page.component'
          ).then((m) => m.SecurityEventsPageComponent),
        title: 'securityEvents.title'
      },
      {
        path: 'monitoring',
        canActivate: [permissionGuard],
        data: { permissions: ['MONITORING_READ'] },
        loadComponent: () =>
          import('@features/monitoring/monitoring-page/monitoring-page.component').then(
            (m) => m.MonitoringPageComponent
          ),
        title: 'monitoring.title'
      },
      {
        path: 'settings',
        canActivate: [permissionGuard],
        data: { permissions: ['SETTINGS_READ'] },
        loadComponent: () =>
          import('@features/settings/settings-page/settings-page.component').then(
            (m) => m.SettingsPageComponent
          ),
        title: 'settings.title'
      }
    ]
  },
  {
    path: '**',
    component: AuthLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@features/authentication/pages/not-found-page/not-found-page.component').then(
            (m) => m.NotFoundPageComponent
          ),
        title: 'common.errors.notFoundTitle'
      }
    ]
  }
];
