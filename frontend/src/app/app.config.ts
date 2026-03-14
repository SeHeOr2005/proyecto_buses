/**
 * =============================================================================
 * app.config — Configuración global de la aplicación Angular
 * =============================================================================
 *
 * Define los providers que estarán disponibles en toda la aplicación.
 * Se usa en main.ts con bootstrapApplication(AppComponent, appConfig).
 *
 * Providers registrados:
 *
 *  provideBrowserGlobalErrorListeners()
 *    Registra listeners globales de errores en el navegador.
 *    Captura errores no manejados (unhandled errors) y los reporta a Angular.
 *
 *  provideRouter(routes)
 *    Configura el sistema de rutas con las rutas definidas en app.routes.ts.
 *    Habilita lazy loading, navegación programática y RouterLink.
 *
 *  provideHttpClient()
 *    Registra HttpClient para hacer peticiones HTTP al backend.
 *    Requerido por AuthService para llamar a los endpoints de la API.
 *    Sin este provider, inyectar HttpClient en cualquier servicio lanzaría un error.
 * =============================================================================
 */

import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(), // Captura errores globales del navegador
    provideRouter(routes),                // Sistema de rutas con lazy loading
    provideHttpClient()                   // Cliente HTTP para llamadas a la API REST
  ]
};
