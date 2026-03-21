/**
 * HomeComponent - Panel principal tras iniciar sesión
 * Muestra el dashboard con cards de módulos (Rutas, Flota, Horarios)
 * Incluye header con email del usuario y botón de cerrar sesión
 * Soporta login por email (localStorage) y OAuth2 (Google)
 */

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService, OAuth2UserData } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  /** Email del usuario */
  userEmail: string = '';
  /** Nombre del usuario */
  userName: string = '';
  /** Foto del usuario */
  userPicture: string = '';
  /** Tipo de sesión: 'oauth2' o 'local' */
  sessionType: 'oauth2' | 'local' = 'local';

  /** Iniciales para avatar cuando no hay foto (ej: "JD" para Juan Díaz). Nunca mostrar "?" */
  getInitials(): string {
    if (this.userName && this.userName.trim()) {
      const parts = this.userName.trim().split(/\s+/).filter(Boolean);
      if (parts.length >= 2) {
        return (parts[0][0] + parts[1][0]).toUpperCase();
      }
      return this.userName.trim().slice(0, 2).toUpperCase();
    }
    if (this.userEmail && this.userEmail.trim()) {
      return this.userEmail.trim().slice(0, 2).toUpperCase();
    }
    return 'U';
  }

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUser();
  }

  private loadUser(): void {
    const token = this.authService.getToken();
    const localEmail = this.authService.getUserEmail();
    if (token && localEmail) {
      this.userEmail = localEmail;
      this.userName = this.authService.getUserName() || '';
      this.userPicture = this.authService.getUserPicture() || '';
      this.sessionType = 'local';
      this.cdr.detectChanges();
      return;
    }
    this.authService.getOAuth2User().subscribe({
      next: (user: OAuth2UserData) => {
        this.userName = user.name || '';
        this.userEmail = user.email || '';
        this.userPicture = user.picture || '';
        this.sessionType = 'oauth2';
        this.cdr.detectChanges();
      },
      error: () => {
        this.userEmail = this.authService.getUserEmail() || '';
        this.userName = this.authService.getUserName() || '';
        this.userPicture = this.authService.getUserPicture() || '';
        this.sessionType = 'local';
        this.cdr.detectChanges();
      }
    });
  }

  /** Cierra sesión y redirige al login */
  onLogout(): void {
    this.authService.logout();
    if (this.sessionType === 'oauth2') {
      window.location.href = 'http://localhost:8080/logout';
    } else {
      this.router.navigate(['/login']);
    }
  }
}
