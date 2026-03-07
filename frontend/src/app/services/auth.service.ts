/**
 * AuthService - Servicio de autenticación
 * Gestiona login, registro, token JWT y datos de usuario en localStorage
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** URL base del API de autenticación */
const API_URL = 'http://localhost:8081/api/auth';

/** Respuesta del endpoint de login */
export interface LoginResponse {
  message: string;
  token: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) {}

  /** Envía credenciales al API y devuelve el token si son válidas */
  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_URL}/login`, { email, password });
  }

  /** Registra un nuevo usuario en el API */
  register(name: string, email: string, password: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${API_URL}/register`, { name, email, password });
  }

  /** Guarda el token JWT en localStorage */
  saveToken(token: string): void {
    localStorage.setItem('token', token);
  }

  /** Obtiene el token JWT del localStorage */
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  /** Comprueba si el usuario tiene sesión activa */
  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  /** Cierra sesión eliminando token y email del localStorage */
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
  }

  /** Guarda el email del usuario en localStorage */
  saveUserEmail(email: string): void {
    localStorage.setItem('userEmail', email);
  }

  /** Obtiene el email del usuario del localStorage */
  getUserEmail(): string | null {
    return localStorage.getItem('userEmail');
  }
}
