/**
 * App - Componente raíz de la aplicación BusTrack
 * Contiene el router-outlet para cargar las rutas (login, home, etc.)
 */

import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  /** Título de la aplicación */
  protected readonly title = signal('frontend');
}
