/**
 * =============================================================================
 * app.routes — Definición de rutas de la aplicación
 * =============================================================================
 *
 * Usa lazy loading (loadComponent) para cargar cada componente solo cuando
 * el usuario navega a esa ruta, reduciendo el bundle inicial.
 *
 * Rutas disponibles:
 * ┌─────────────────┬──────────────────────────────────────────────────────┐
 * │ Ruta            │ Descripción                                          │
 * ├─────────────────┼──────────────────────────────────────────────────────┤
 * │ /               │ Redirige a /login                                    │
 * │ /login          │ Formulario de login (AuthComponent, panel izquierdo) │
 * │ /registro       │ Formulario de registro (AuthComponent, panel derecho)│
 * │ /home           │ Página principal tras autenticarse                   │
 * │ /auth/callback  │ Callback OAuth2 (GitHub): procesa token y redirige   │
 * └─────────────────┴──────────────────────────────────────────────────────┘
 *
 * Nota: /login y /registro usan el mismo AuthComponent.
 * La diferencia es que /registro pasa data: { showRegister: true },
 * lo que hace que el componente active el panel de registro al iniciar.
 * =============================================================================
 */

import { Routes } from '@angular/router';

export const routes: Routes = [
  /**
   * Ruta raíz: redirige automáticamente a /login.
   * pathMatch: 'full' asegura que solo coincida con la ruta exacta '/'.
   */
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },

  /**
   * Ruta de login: carga AuthComponent con el panel de inicio de sesión visible.
   * El componente detecta que no hay data.showRegister y muestra el panel izquierdo.
   */
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/auth/auth.component').then(m => m.AuthComponent)
  },

  /**
   * Ruta de registro: carga el mismo AuthComponent pero con data.showRegister = true.
   * El componente lee este dato en el constructor y activa el panel de registro.
   * Permite navegar directamente a /registro desde un enlace externo.
   */
  {
    path: 'registro',
    loadComponent: () =>
      import('./pages/auth/auth.component').then(m => m.AuthComponent),
    data: { showRegister: true }
  },

  /**
   * Ruta del home: página principal de la aplicación.
   * Solo accesible tras autenticarse (el componente puede verificar el token).
   */
  {
    path: 'home',
    loadComponent: () =>
      import('./pages/home/home.component').then(m => m.HomeComponent)
  },

  /**
   * Callback OAuth2 para GitHub (y potencialmente otros proveedores).
   * El backend redirige aquí tras el login social con query params:
   *   ?token=<sessionToken>&name=<nombre>&email=<email>&picture=<url>
   * El AuthCallbackComponent lee los params, guarda la sesión y redirige a /home.
   */
  {
    path: 'auth/callback',
    loadComponent: () =>
      import('./pages/auth-callback/auth-callback.component').then(m => m.AuthCallbackComponent)
  }
];
