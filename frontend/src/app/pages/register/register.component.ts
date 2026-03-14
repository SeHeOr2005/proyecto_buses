import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

export type PasswordStrength = 'weak' | 'medium' | 'strong';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  name = '';
  lastName = '';
  email = '';
  password = '';
  confirmPassword = '';
  errorMessage = '';
  successMessage = '';
  isRegistering = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  /** Indicador visual de fortaleza: débil, media, fuerte (según requisitos HU) */
  getPasswordStrength(pwd: string): PasswordStrength {
    if (!pwd || pwd.length === 0) return 'weak';
    const hasLower = /[a-z]/.test(pwd);
    const hasUpper = /[A-Z]/.test(pwd);
    const hasNumber = /\d/.test(pwd);
    const hasSpecial = /[^A-Za-z0-9]/.test(pwd);
    const count = [hasLower, hasUpper, hasNumber, hasSpecial].filter(Boolean).length;
    if (pwd.length >= 12 && count === 4) return 'strong';
    if (pwd.length >= 8 && count >= 3) return 'medium';
    return 'weak';
  }

  /** Etiqueta para mostrar en la UI */
  getStrengthLabel(pwd: string): string {
    const s = this.getPasswordStrength(pwd);
    return s === 'weak' ? 'Débil' : s === 'medium' ? 'Media' : 'Fuerte';
  }

  /** Cumple requisitos: mínimo 8 caracteres, mayúscula, minúscula, número, carácter especial */
  passwordMeetsRequirements(pwd: string): boolean {
    return !!pwd && pwd.length >= 8
      && /[a-z]/.test(pwd) && /[A-Z]/.test(pwd)
      && /\d/.test(pwd) && /[^A-Za-z0-9]/.test(pwd);
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const nameTrim = (this.name || '').trim();
    const lastNameTrim = (this.lastName || '').trim();
    const emailTrim = (this.email || '').trim();

    if (!nameTrim || !lastNameTrim || !emailTrim) {
      this.errorMessage = 'Complete todos los campos obligatorios: nombre, apellido y correo electrónico.';
      return;
    }

    if (!this.password || !this.confirmPassword) {
      this.errorMessage = 'Debe ingresar la contraseña y su confirmación.';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden';
      return;
    }

    if (!this.passwordMeetsRequirements(this.password)) {
      this.errorMessage = 'La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial';
      return;
    }

    this.isRegistering = true;
    this.authService.register(nameTrim, lastNameTrim, emailTrim, this.password).subscribe({
      next: () => {
        this.isRegistering = false;
        this.successMessage = 'Registro exitoso. Iniciando sesión automáticamente...';
        // Hacer login automático después de registro
        this.authService.login(emailTrim, this.password).subscribe({
          next: (loginResponse) => {
            this.authService.saveLoginData(loginResponse);
            this.router.navigate(['/home']);
          },
          error: () => {
            this.errorMessage = 'Registro exitoso, pero error al iniciar sesión. Intente manualmente.';
            setTimeout(() => this.router.navigate(['/login']), 2000);
          }
        });
      },
      error: (err) => {
        this.isRegistering = false;
        this.errorMessage = this.getRegisterErrorMessage(err);
      }
    });
  }

  private getRegisterErrorMessage(err: { error?: { message?: string }; message?: string }): string {
    if (err?.error?.message) return err.error.message;
    if (typeof err?.error === 'string') return err.error;
    if (err?.message) return err.message;
    if (err?.error && typeof err.error === 'object') {
      const msg = (err.error as { message?: string; errors?: string[] }).message
        || ((err.error as { errors?: string[] }).errors?.join('. '));
      if (msg) return msg;
    }
    return 'No se pudo conectar con el servidor. Compruebe que el backend esté en ejecución en http://localhost:8081';
  }
}
