/**
 * AuthComponent - Componente de autenticación
 * Gestiona login y registro en una vista de dos paneles deslizantes.
 * Incluye login por email y botones de redes sociales (Google, Microsoft, GitHub).
 */

import { Component } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css'
})
export class AuthComponent {
  /** Indica si se muestra el formulario de registro (true) o login (false) */
  showRegister = false;

  /** Email para login */
  email = '';
  /** Contraseña para login */
  password = '';
  /** Mensaje de error en login */
  errorMessage = '';

  /** Nombre completo para registro */
  name = '';
  /** Email para registro */
  regEmail = '';
  /** Contraseña para registro */
  regPassword = '';
  /** Confirmación de contraseña */
  regConfirmPassword = '';
  /** Mensaje de error en registro */
  regErrorMessage = '';
  /** Mensaje de éxito en registro */
  regSuccessMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Si se navega a /registro, mostrar el formulario de registro directamente
    if (this.route.snapshot.data['showRegister']) {
      this.showRegister = true;
    }
  }

  /**
   * Alterna entre el formulario de login y registro.
   * Limpia los mensajes de error al cambiar.
   */
  togglePanel(): void {
    this.showRegister = !this.showRegister;
    this.errorMessage = '';
    this.regErrorMessage = '';
    this.regSuccessMessage = '';
  }

  /**
   * Maneja el clic en botones de login social.
   * Por ahora muestra mensaje de "próximamente".
   */
  onSocialLogin(provider: string): void {
    this.errorMessage = `${provider}: disponible próximamente`;
  }

  /**
   * Envía el formulario de login.
   * Llama al AuthService y navega a /home si es exitoso.
   */
  onLoginSubmit(): void {
    this.errorMessage = '';
    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        this.authService.saveToken(response.token);
        this.authService.saveUserEmail(this.email);
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.errorMessage = err.error || 'Email o contraseña incorrectos';
      }
    });
  }

  /**
   * Envía el formulario de registro.
   * Valida que las contraseñas coincidan y tengan al menos 6 caracteres.
   * Redirige a /login tras registro exitoso.
   */
  onRegisterSubmit(): void {
    this.regErrorMessage = '';
    this.regSuccessMessage = '';

    // Validar que las contraseñas coincidan
    if (this.regPassword !== this.regConfirmPassword) {
      this.regErrorMessage = 'Las contraseñas no coinciden';
      return;
    }

    // Validar longitud mínima de contraseña
    if (this.regPassword.length < 6) {
      this.regErrorMessage = 'La contraseña debe tener al menos 6 caracteres';
      return;
    }

    this.authService.register(this.name, this.regEmail, this.regPassword).subscribe({
      next: () => {
        this.regSuccessMessage = 'Registro exitoso. Redirigiendo...';
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.regErrorMessage = err.error?.message || err.error || 'Error al registrar';
      }
    });
  }
}
