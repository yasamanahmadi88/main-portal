import { Routes } from '@angular/router';

export const ROLES_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./pages/roles-list-page/roles-list-page.component').then(
        (m) => m.RolesListPageComponent
      ),
    title: 'roles.title'
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./pages/role-form-page/role-form-page.component').then(
        (m) => m.RoleFormPageComponent
      ),
    title: 'roles.actions.create'
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/role-detail-page/role-detail-page.component').then(
        (m) => m.RoleDetailPageComponent
      )
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./pages/role-form-page/role-form-page.component').then(
        (m) => m.RoleFormPageComponent
      )
  }
];
