/**
 * app.config - Configuración global de la aplicación Angular
 * Define los providers: router, HttpClient, manejador de errores
 */

import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { routes } from './app.routes';

/** Configuración de la aplicación con todos los providers necesarios */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),  // Manejador global de errores
    provideRouter(routes),                 // Sistema de rutas
    provideHttpClient()                    // Cliente HTTP para llamadas a API
  ]
};
