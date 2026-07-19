import { Routes } from '@angular/router';

export const USERS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./pages/users-list-page/users-list-page.component').then(
        (m) => m.UsersListPageComponent
      ),
    title: 'users.title'
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./pages/user-form-page/user-form-page.component').then(
        (m) => m.UserFormPageComponent
      ),
    title: 'users.actions.create'
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/user-detail-page/user-detail-page.component').then(
        (m) => m.UserDetailPageComponent
      )
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./pages/user-form-page/user-form-page.component').then(
        (m) => m.UserFormPageComponent
      )
  }
];
