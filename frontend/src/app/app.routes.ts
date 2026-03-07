/**
 * app.routes - Definición de rutas de la aplicación
 * Usa lazy loading para cargar componentes bajo demanda
 */

import { Routes } from '@angular/router';

export const routes: Routes = [
  // Ruta raíz: redirige a login
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  // Login: formulario de inicio de sesión
  { path: 'login', loadComponent: () => import('./pages/auth/auth.component').then(m => m.AuthComponent) },
  // Registro: mismo componente Auth pero mostrando el formulario de registro
  { path: 'registro', loadComponent: () => import('./pages/auth/auth.component').then(m => m.AuthComponent), data: { showRegister: true } },
  // Home: panel principal tras iniciar sesión
  { path: 'home', loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent) },
  // Callback OAuth (GitHub): recibe token, name, email, picture y redirige a home
  { path: 'auth/callback', loadComponent: () => import('./pages/auth-callback/auth-callback.component').then(m => m.AuthCallbackComponent) }
];
