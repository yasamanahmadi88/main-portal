import { Routes } from '@angular/router';

export const AUDIT_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./pages/audit-list-page/audit-list-page.component').then(
        (m) => m.AuditListPageComponent
      ),
    title: 'audit.title'
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/audit-detail-page/audit-detail-page.component').then(
        (m) => m.AuditDetailPageComponent
      )
  }
];
