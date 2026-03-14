/**
 * =============================================================================
 * AuthComponent — Componente de autenticación (Login + Registro)
 * =============================================================================
 *
 * Vista única con dos paneles deslizantes:
 *  - Panel izquierdo (sign-in-container): formulario de inicio de sesión
 *  - Panel derecho  (sign-up-container): formulario de registro
 *
 * La clase CSS `right-panel-active` en `.auth-page` activa la animación
 * que desliza el overlay y muestra el formulario de registro.
 *
 * Rutas que usan este componente:
 *  /login    → muestra el formulario de login por defecto
 *  /registro → muestra el formulario de registro (data: { showRegister: true })
 *
 * Flujo de REGISTRO:
 *  1. Usuario llena: nombre, apellido, email, contraseña, confirmación
 *  2. onRegisterSubmit() valida los campos en el frontend
 *  3. Se llama a AuthService.register() → POST /api/auth/register
 *  4. Si el backend responde 200 OK:
 *     a. Se hace login automático con las mismas credenciales
 *     b. Se guarda el token y datos en localStorage
 *     c. Se redirige a /home
 *  5. Si el backend responde error (ej. email duplicado):
 *     a. Se muestra el mensaje de error del backend
 *
 * Flujo de LOGIN:
 *  1. Usuario ingresa email y contraseña
 *  2. onLoginSubmit() llama a AuthService.login() → POST /api/auth/login
 *  3. Si el backend responde 200 OK:
 *     a. Se guarda token, email, nombre y foto en localStorage
 *     b. Se redirige a /home
 *  4. Si el backend responde 401:
 *     a. Se muestra "Email o contraseña incorrectos"
 *
 * Login social:
 *  - Google  → redirige a http://localhost:8081/oauth2/authorization/google
 *  - GitHub  → redirige a GitHub OAuth con client_id del backend
 *  - Microsoft → muestra "próximamente"
 * =============================================================================
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

  // ─────────────────────────────────────────────────────────────────────────
  // ESTADO DEL PANEL
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Controla qué panel se muestra.
   * false → panel de login (por defecto)
   * true  → panel de registro
   * Vinculado a la clase CSS `right-panel-active` en el template.
   */
  showRegister = false;

  // ─────────────────────────────────────────────────────────────────────────
  // CAMPOS DEL FORMULARIO DE LOGIN
  // ─────────────────────────────────────────────────────────────────────────

  /** Email ingresado en el formulario de login */
  email = '';

  /** Contraseña ingresada en el formulario de login */
  password = '';

  /** Mensaje de error que se muestra bajo el formulario de login */
  errorMessage = '';

  // ─────────────────────────────────────────────────────────────────────────
  // CAMPOS DEL FORMULARIO DE REGISTRO
  // ─────────────────────────────────────────────────────────────────────────

  /** Nombre del usuario en el formulario de registro */
  name = '';

  /** Apellido del usuario en el formulario de registro */
  regLastName = '';

  /** Email del usuario en el formulario de registro */
  regEmail = '';

  /** Contraseña elegida en el formulario de registro */
  regPassword = '';

  /** Confirmación de contraseña (debe coincidir con regPassword) */
  regConfirmPassword = '';

  /** Mensaje de error que se muestra bajo el formulario de registro */
  regErrorMessage = '';

  /** Mensaje de éxito que se muestra cuando el registro fue exitoso */
  regSuccessMessage = '';

  /**
   * Bandera que se activa mientras se procesa el registro.
   * Deshabilita el botón "REGISTRAR" para evitar envíos duplicados
   * y cambia su texto a "Registrando...".
   */
  isRegistering = false;

  // ─────────────────────────────────────────────────────────────────────────
  // CONSTRUCTOR
  // ─────────────────────────────────────────────────────────────────────────

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    /**
     * Si la ruta tiene data: { showRegister: true } (ruta /registro),
     * se activa el panel de registro directamente al cargar el componente.
     */
    if (this.route.snapshot.data['showRegister']) {
      this.showRegister = true;
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // CONTROL DEL PANEL
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Alterna entre el panel de login y el de registro.
   * También limpia todos los mensajes de error y éxito para que no
   * queden mensajes del panel anterior visibles al cambiar.
   *
   * Llamado por los botones "Registrar" e "Iniciar Sesión" del overlay.
   */
  togglePanel(): void {
    this.showRegister = !this.showRegister;
    this.errorMessage = '';
    this.regErrorMessage = '';
    this.regSuccessMessage = '';
  }

  // ─────────────────────────────────────────────────────────────────────────
  // LOGIN SOCIAL (OAuth2)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Inicia el flujo OAuth2 con Google.
   * Redirige al endpoint del backend que inicia la autorización con Google.
   * Tras autenticarse, Google redirige al backend que a su vez redirige
   * al frontend en /home con la sesión establecida.
   */
  loginGoogle(): void {
    window.location.href = 'http://localhost:8081/oauth2/authorization/google';
  }

  /**
   * Inicia el flujo OAuth2 con GitHub.
   * Redirige directamente a GitHub con el client_id de la app registrada.
   * GitHub redirige al backend en /auth/github/callback con el código de autorización.
   * El backend intercambia el código por un token, crea/actualiza el usuario en MongoDB
   * y redirige al frontend en /auth/callback con el token de sesión como query param.
   */
  loginGitHub(): void {
    const clientId = 'Ov23ligZM0UoBxmlXOSG';
    const redirectUri = encodeURIComponent('http://localhost:8081/auth/github/callback');
    window.location.href = `https://github.com/login/oauth/authorize?client_id=${clientId}&redirect_uri=${redirectUri}`;
  }

  /**
   * Dispatcher para los botones de login social.
   * Según el proveedor recibido, llama al método correspondiente.
   *
   * @param provider 'google' | 'github' | 'microsoft'
   */
  onSocialLogin(provider: string): void {
    if (provider.toLowerCase() === 'google') {
      this.loginGoogle();
    } else if (provider.toLowerCase() === 'github') {
      this.loginGitHub();
    } else {
      // Microsoft y otros proveedores aún no implementados
      this.errorMessage = `${provider}: disponible próximamente`;
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FORMULARIO DE LOGIN
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Maneja el envío del formulario de inicio de sesión.
   *
   * Pasos:
   *  1. Limpia el mensaje de error anterior
   *  2. Llama a AuthService.login() con email y contraseña
   *  3. En caso de éxito:
   *     - Guarda token, email, nombre y foto en localStorage
   *     - Navega a /home
   *  4. En caso de error:
   *     - Muestra el mensaje de error del backend o uno genérico
   *
   * Vinculado al evento (ngSubmit) del formulario de login en el template.
   */
  onLoginSubmit(): void {
    this.errorMessage = '';

    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        // Persistir datos de sesión en localStorage
        this.authService.saveToken(response.token);
        this.authService.saveUserEmail(response.email ?? this.email);
        this.authService.saveUserName(response.name ?? '');
        this.authService.saveUserPicture(response.picture ?? null);
        // Navegar al home (setTimeout 0 para asegurar que Angular procese el cambio)
        setTimeout(() => this.router.navigate(['/home']), 0);
      },
      error: (err) => {
        // Intentar extraer el mensaje del backend; si no, usar mensaje genérico
        this.errorMessage = err?.error?.message || err?.error || 'Email o contraseña incorrectos';
      }
    });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // INDICADOR DE FORTALEZA DE CONTRASEÑA
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Calcula la fortaleza de la contraseña ingresada en el registro.
   *
   * Criterios:
   *  - 'weak'   → menos de 8 caracteres o menos de 3 tipos de caracteres
   *  - 'medium' → 8+ caracteres y al menos 3 de: minúscula, mayúscula, número, especial
   *  - 'strong' → 12+ caracteres y los 4 tipos de caracteres presentes
   *
   * @param pwd Contraseña a evaluar
   * @returns 'weak' | 'medium' | 'strong'
   */
  getRegPasswordStrength(pwd: string): 'weak' | 'medium' | 'strong' {
    if (!pwd?.length) return 'weak';
    const hasLower   = /[a-z]/.test(pwd);
    const hasUpper   = /[A-Z]/.test(pwd);
    const hasNumber  = /\d/.test(pwd);
    const hasSpecial = /[^A-Za-z0-9]/.test(pwd);
    const count = [hasLower, hasUpper, hasNumber, hasSpecial].filter(Boolean).length;
    if (pwd.length >= 12 && count === 4) return 'strong';
    if (pwd.length >= 8  && count >= 3)  return 'medium';
    return 'weak';
  }

  /**
   * Devuelve la etiqueta en español para mostrar junto a la barra de fortaleza.
   * @param pwd Contraseña a evaluar
   * @returns 'Débil' | 'Media' | 'Fuerte'
   */
  getRegStrengthLabel(pwd: string): string {
    const s = this.getRegPasswordStrength(pwd);
    return s === 'weak' ? 'Débil' : s === 'medium' ? 'Media' : 'Fuerte';
  }

  /**
   * Verifica si la contraseña cumple todos los requisitos mínimos de seguridad:
   *  - Mínimo 8 caracteres
   *  - Al menos una letra minúscula
   *  - Al menos una letra mayúscula
   *  - Al menos un número
   *  - Al menos un carácter especial (no alfanumérico)
   *
   * @param pwd Contraseña a validar
   * @returns true si cumple todos los requisitos, false en caso contrario
   */
  regPasswordMeetsRequirements(pwd: string): boolean {
    return (
      !!pwd &&
      pwd.length >= 8 &&
      /[a-z]/.test(pwd) &&
      /[A-Z]/.test(pwd) &&
      /\d/.test(pwd) &&
      /[^A-Za-z0-9]/.test(pwd)
    );
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FORMULARIO DE REGISTRO
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Maneja el envío del formulario de registro.
   *
   * Validaciones en el frontend (antes de llamar al backend):
   *  1. Nombre, apellido y email no pueden estar vacíos
   *  2. Contraseña y confirmación no pueden estar vacías
   *  3. Contraseña y confirmación deben coincidir
   *  4. La contraseña debe cumplir los requisitos de seguridad
   *
   * Flujo tras validación exitosa:
   *  1. Activa isRegistering = true (deshabilita el botón)
   *  2. Llama a AuthService.register() → POST /api/auth/register
   *  3. Si el backend responde 200 OK (usuario creado en MongoDB):
   *     a. Hace login automático con las mismas credenciales
   *     b. Guarda token y datos en localStorage
   *     c. Muestra "¡Cuenta creada! Ingresando..." y redirige a /home
   *  4. Si el auto-login falla (caso raro):
   *     a. Muestra mensaje de éxito del registro
   *     b. Cambia al panel de login para que el usuario ingrese manualmente
   *  5. Si el backend responde error (ej. 409 email duplicado):
   *     a. Muestra el mensaje de error del backend
   *
   * Vinculado al evento (ngSubmit) del formulario de registro en el template.
   */
  onRegisterSubmit(): void {
    this.regErrorMessage = '';
    this.regSuccessMessage = '';

    // Normalizar espacios en los campos de texto
    const nameTrim     = (this.name       || '').trim();
    const lastNameTrim = (this.regLastName || '').trim();
    const emailTrim    = (this.regEmail    || '').trim();

    // Validación 1: campos obligatorios
    if (!nameTrim || !lastNameTrim || !emailTrim) {
      this.regErrorMessage = 'Complete todos los campos obligatorios: nombre, apellido y correo electrónico.';
      return;
    }

    // Validación 2: contraseñas no vacías
    if (!this.regPassword || !this.regConfirmPassword) {
      this.regErrorMessage = 'Debe ingresar la contraseña y su confirmación.';
      return;
    }

    // Validación 3: contraseñas coinciden
    if (this.regPassword !== this.regConfirmPassword) {
      this.regErrorMessage = 'Las contraseñas no coinciden';
      return;
    }

    // Validación 4: requisitos de seguridad de la contraseña
    if (!this.regPasswordMeetsRequirements(this.regPassword)) {
      this.regErrorMessage = 'La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial';
      return;
    }

    // Activar estado de carga
    this.isRegistering = true;

    // Llamar al backend para registrar el usuario
    this.authService.register(nameTrim, lastNameTrim, emailTrim, this.regPassword).subscribe({
      next: () => {
        /**
         * Registro exitoso en MongoDB.
         * Hacer login automático con las mismas credenciales para que el usuario
         * entre directamente a /home sin tener que volver a escribir sus datos.
         */
        this.authService.login(emailTrim, this.regPassword).subscribe({
          next: (response) => {
            this.isRegistering = false;
            // Persistir sesión en localStorage
            this.authService.saveToken(response.token);
            this.authService.saveUserEmail(response.email ?? emailTrim);
            this.authService.saveUserName(response.name ?? nameTrim);
            this.authService.saveUserPicture(response.picture ?? null);
            // Mostrar mensaje de éxito y redirigir
            this.regSuccessMessage = '¡Cuenta creada! Ingresando...';
            setTimeout(() => this.router.navigate(['/home']), 1000);
          },
          error: () => {
            /**
             * El auto-login falló (caso poco probable).
             * Mostrar mensaje de éxito del registro y cambiar al panel de login
             * para que el usuario ingrese manualmente sus credenciales.
             */
            this.isRegistering = false;
            this.regSuccessMessage = 'Registro exitoso. Inicia sesión para continuar.';
            setTimeout(() => {
              this.showRegister = false; // Cambiar al panel de login
            }, 1500);
          }
        });
      },
      error: (err) => {
        // El registro falló (ej. email ya registrado, validación del backend)
        this.isRegistering = false;
        this.regErrorMessage = this.getRegisterErrorMessage(err);
      }
    });
  }

  /**
   * Extrae el mensaje de error más descriptivo de la respuesta de error HTTP.
   *
   * Intenta obtener el mensaje en este orden de prioridad:
   *  1. err.error.message  → objeto JSON del backend con campo "message"
   *  2. err.error          → string directo del backend
   *  3. err.message        → mensaje del objeto Error de JavaScript
   *  4. err.error.errors   → array de errores de validación (Bean Validation)
   *  5. Mensaje genérico   → indica que el backend no está disponible
   *
   * @param err Objeto de error de HttpClient
   * @returns Mensaje de error legible para el usuario
   */
  private getRegisterErrorMessage(err: { error?: { message?: string; errors?: string[] }; message?: string }): string {
    if (err?.error?.message) return err.error.message;
    if (typeof err?.error === 'string') return err.error;
    if (err?.message) return err.message;
    if (err?.error?.errors?.length) return err.error.errors.join('. ');
    return 'No se pudo conectar con el servidor. Compruebe que el backend esté en ejecución en http://localhost:8081';
  }
}
