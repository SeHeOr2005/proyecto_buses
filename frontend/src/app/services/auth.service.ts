/**
 * =============================================================================
 * AuthService — Servicio central de autenticación
 * =============================================================================
 *
 * Responsabilidades:
 *  1. Comunicarse con el backend (Spring Boot en http://localhost:8081) para:
 *     - Registrar nuevos usuarios  → POST /api/auth/register
 *     - Iniciar sesión             → POST /api/auth/login
 *     - Obtener perfil propio      → GET  /api/auth/me  (requiere token)
 *     - Cambiar contraseña         → POST /api/auth/change-password (requiere token)
 *     - Obtener usuario OAuth2     → GET  /user  (sesión de cookie)
 *
 *  2. Persistir en localStorage:
 *     - 'token'       → token de sesión devuelto por el backend tras login
 *     - 'userEmail'   → email del usuario autenticado
 *     - 'userName'    → nombre del usuario autenticado
 *     - 'userPicture' → URL de foto de perfil (OAuth2 o null)
 *
 * Flujo de autenticación:
 *  Registro → backend guarda usuario con contraseña hasheada (bcrypt) en MongoDB
 *  Login    → backend verifica hash, genera sessionToken, lo guarda en el usuario
 *             y lo devuelve al frontend → frontend lo guarda en localStorage
 *  Rutas protegidas → frontend envía token como "Authorization: Bearer <token>"
 *  Logout   → frontend elimina token y datos del localStorage
 * =============================================================================
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** URL base del API de autenticación en el backend */
const API_URL = 'http://localhost:8080/api/auth';

/** URL base del backend (para endpoints OAuth2) */
const API_BASE = 'http://localhost:8080';

/**
 * Estructura de la respuesta del endpoint POST /api/auth/login.
 * El campo `token` es el sessionToken que se debe guardar en localStorage.
 */
export interface LoginResponse {
  /** Mensaje descriptivo del resultado ("Login exitoso") */
  message: string;
  /** Token de sesión único generado por el backend; se usa como Bearer token */
  token: string;
  /** Nombre del usuario (opcional, viene del documento User en MongoDB) */
  name?: string;
  /** Email del usuario (opcional, confirmación del email usado) */
  email?: string;
  /** URL de foto de perfil (solo para usuarios OAuth2; null para registro manual) */
  picture?: string | null;
}

/**
 * Estructura de los datos del usuario autenticado via OAuth2 (Google, GitHub).
 * Se obtiene llamando a GET /user con credenciales de sesión (cookie).
 */
export interface OAuth2UserData {
  /** Subject: identificador único del proveedor OAuth2 */
  sub?: string;
  /** Nombre completo del usuario */
  name?: string;
  /** Email del usuario */
  email?: string;
  /** URL de foto de perfil */
  picture?: string;
  /** Nombre de pila (Google) */
  given_name?: string;
  /** Apellido (Google) */
  family_name?: string;
  /** Si el email fue verificado por el proveedor */
  email_verified?: boolean;
}

@Injectable({
  providedIn: 'root' // Singleton disponible en toda la aplicación
})
export class AuthService {

  constructor(private http: HttpClient) {}

  // ─────────────────────────────────────────────────────────────────────────
  // MÉTODOS DE API
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Obtiene los datos del usuario autenticado via OAuth2 desde el backend.
   * Requiere que exista una sesión activa (cookie de sesión de Spring Security).
   * Se usa en el callback de OAuth2 para obtener nombre, email y foto.
   */
  getOAuth2User(): Observable<OAuth2UserData> {
    return this.http.get<OAuth2UserData>(`${API_BASE}/user`, { withCredentials: true });
  }

  /**
   * Inicia sesión con email y contraseña.
   *
   * Envía POST /api/auth/login con { email, password }.
   * El backend busca el usuario en MongoDB, verifica la contraseña con bcrypt,
   * genera un sessionToken único (UUID), lo guarda en el documento del usuario
   * y lo devuelve en la respuesta.
   *
   * @param email    Email del usuario (se normaliza a minúsculas en el backend)
   * @param password Contraseña en texto plano (el backend la compara con el hash)
   * @returns Observable con LoginResponse que contiene el token de sesión
   */
  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_URL}/login`, { email, password });
  }

  /**
   * Registra un nuevo usuario en el sistema.
   *
   * Envía POST /api/auth/register con { name, lastName, email, password }.
   * El backend verifica que el email no exista, hashea la contraseña con bcrypt
   * y guarda el nuevo documento User en MongoDB.
   *
   * Validaciones del backend:
   *  - name, lastName, email: no pueden estar vacíos
   *  - email: debe tener formato válido
   *  - password: mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial
   *  - email: debe ser único (no puede haber dos usuarios con el mismo email)
   *
   * @param name     Nombre del usuario
   * @param lastName Apellido del usuario
   * @param email    Email único del usuario
   * @param password Contraseña que cumple los requisitos de seguridad
   * @returns Observable con { message: "Registro exitoso" } o error HTTP 409 si el email ya existe
   */
  register(name: string, lastName: string, email: string, password: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${API_URL}/register`, { name, lastName, email, password });
  }

  /**
   * Obtiene el perfil del usuario autenticado actualmente.
   *
   * Envía GET /api/auth/me con el token Bearer en el header Authorization.
   * El backend valida el token contra el sessionToken guardado en MongoDB.
   * Si el token no existe o fue invalidado (cambio de contraseña), responde 401.
   *
   * @returns Observable con los datos del usuario: id, email, name, lastName, picture
   */
  getMe(): Observable<{ id: string; email: string; name: string; lastName: string; picture: string }> {
    return this.http.get<{ id: string; email: string; name: string; lastName: string; picture: string }>(
      `${API_URL}/me`,
      { headers: this.authHeaders() }
    );
  }

  /**
   * Cambia la contraseña del usuario autenticado.
   *
   * Envía POST /api/auth/change-password con { oldPassword, newPassword }.
   * El backend verifica la contraseña actual, valida los requisitos de la nueva,
   * la hashea con bcrypt, invalida el sessionToken actual (fuerza re-login)
   * y guarda los cambios en MongoDB.
   *
   * @param oldPassword Contraseña actual del usuario
   * @param newPassword Nueva contraseña (debe cumplir los mismos requisitos de seguridad)
   * @returns Observable con { message: "Contraseña actualizada. Debe iniciar sesión de nuevo." }
   */
  changePassword(oldPassword: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${API_URL}/change-password`,
      { oldPassword, newPassword },
      { headers: this.authHeaders() }
    );
  }

  /**
   * Construye el header Authorization con el token Bearer almacenado.
   * Si no hay token, devuelve un objeto vacío (la petición irá sin autenticación).
   */
  private authHeaders(): { [key: string]: string } {
    const token = this.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  // ─────────────────────────────────────────────────────────────────────────
  // GESTIÓN DE SESIÓN EN localStorage
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Guarda el token de sesión en localStorage bajo la clave 'token'.
   * Este token se envía en cada petición a rutas protegidas del backend.
   */
  saveToken(token: string): void {
    localStorage.setItem('token', token);
  }

  /**
   * Recupera el token de sesión del localStorage.
   * @returns El token si existe, o null si no hay sesión activa.
   */
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  /**
   * Verifica si el usuario tiene una sesión activa comprobando si existe un token.
   * Nota: no valida la vigencia del token contra el backend; solo comprueba su existencia.
   * @returns true si hay token en localStorage, false en caso contrario.
   */
  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  /**
   * Cierra la sesión del usuario eliminando todos sus datos del localStorage.
   * Después de llamar a este método, isLoggedIn() devolverá false.
   */
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userName');
    localStorage.removeItem('userPicture');
  }

  /** Persiste el email del usuario en localStorage bajo la clave 'userEmail'. */
  saveUserEmail(email: string): void {
    localStorage.setItem('userEmail', email);
  }

  /** Recupera el email del usuario del localStorage. */
  getUserEmail(): string | null {
    return localStorage.getItem('userEmail');
  }

  /** Persiste el nombre del usuario en localStorage bajo la clave 'userName'. */
  saveUserName(name: string): void {
    localStorage.setItem('userName', name);
  }

  /** Recupera el nombre del usuario del localStorage. */
  getUserName(): string | null {
    return localStorage.getItem('userName');
  }

  /**
   * Persiste la URL de la foto de perfil en localStorage bajo la clave 'userPicture'.
   * Si picture es null o undefined, elimina la clave del localStorage.
   */
  saveUserPicture(picture: string | null): void {
    if (picture) {
      localStorage.setItem('userPicture', picture);
    } else {
      localStorage.removeItem('userPicture');
    }
  }

  /**
   * Guarda los datos del login en localStorage.
   * @param response Respuesta del login con token, name, email, picture.
   */
  saveLoginData(response: LoginResponse): void {
    this.saveToken(response.token);
    if (response.email) this.saveUserEmail(response.email);
    if (response.name) this.saveUserName(response.name);
    if (response.picture !== undefined) this.saveUserPicture(response.picture);
  }

  /** Recupera la URL de la foto de perfil del localStorage. */
  getUserPicture(): string | null {
    return localStorage.getItem('userPicture');
  }
}
