import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

/** Routes de l'application. Chaque page est chargée à la demande. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'projects' },
  {
    path: 'login',
    title: 'Connexion · PMT',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    title: 'Inscription · PMT',
    loadComponent: () => import('./pages/register/register').then((m) => m.Register),
  },
  {
    path: 'projects',
    title: 'Mes projets · PMT',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/projects/projects').then((m) => m.Projects),
  },
  {
    path: 'projects/:id',
    title: 'Tableau de bord · PMT',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/project-dashboard/project-dashboard').then((m) => m.ProjectDashboard),
  },
  {
    path: 'projects/:id/tasks/:taskId',
    title: 'Tâche · PMT',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/task-detail/task-detail').then((m) => m.TaskDetail),
  },
  { path: '**', redirectTo: 'projects' },
];
