/**
 * HomeComponent - Panel principal tras iniciar sesión
 * Muestra el dashboard con cards de módulos (Rutas, Flota, Horarios)
 * Incluye header con email del usuario y botón de cerrar sesión
 */

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  /** Email del usuario logueado (obtenido del AuthService) */
  userEmail: string | null = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.userEmail = this.authService.getUserEmail();
  }

  /** Cierra sesión y redirige al login */
  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
