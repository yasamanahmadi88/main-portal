import { Routes } from '@angular/router';

export const PROFILE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/profile-page/profile-page.component').then((m) => m.ProfilePageComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'overview'
      },
      {
        path: 'overview',
        loadComponent: () =>
          import('./pages/profile-overview-page/profile-overview-page.component').then(
            (m) => m.ProfileOverviewPageComponent
          )
      },
      {
        path: 'security',
        loadComponent: () =>
          import('./pages/profile-security-page/profile-security-page.component').then(
            (m) => m.ProfileSecurityPageComponent
          )
      },
      {
        path: 'sessions',
        loadComponent: () =>
          import('./pages/profile-sessions-page/profile-sessions-page.component').then(
            (m) => m.ProfileSessionsPageComponent
          )
      }
    ]
  }
];
