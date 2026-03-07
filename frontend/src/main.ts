/**
 * main.ts - Punto de entrada de la aplicación Angular
 * Inicializa la app con el componente raíz y la configuración
 */

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
