/**
 * AuthService - Servicio de autenticación
 * Gestiona login, registro, token JWT y datos de usuario en localStorage
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** URL base del API de autenticación */
const API_URL = 'http://localhost:8081/api/auth';
const API_BASE = 'http://localhost:8081';

/** Respuesta del endpoint de login */
export interface LoginResponse {
  message: string;
  token: string;
  name?: string;
  email?: string;
  picture?: string | null;
}

/** Datos del usuario OAuth2 (Google, etc.) */
export interface OAuth2UserData {
  sub?: string;
  name?: string;
  email?: string;
  picture?: string;
  given_name?: string;
  family_name?: string;
  email_verified?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) {}

  /** Obtiene los datos del usuario desde el backend (sesión OAuth2) */
  getOAuth2User(): Observable<OAuth2UserData> {
    return this.http.get<OAuth2UserData>(`${API_BASE}/user`, { withCredentials: true });
  }

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

  /** Cierra sesión eliminando token y datos de usuario del localStorage */
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userName');
    localStorage.removeItem('userPicture');
  }

  /** Guarda el email del usuario en localStorage */
  saveUserEmail(email: string): void {
    localStorage.setItem('userEmail', email);
  }

  /** Obtiene el email del usuario del localStorage */
  getUserEmail(): string | null {
    return localStorage.getItem('userEmail');
  }

  /** Guarda el nombre del usuario en localStorage */
  saveUserName(name: string): void {
    localStorage.setItem('userName', name);
  }

  /** Obtiene el nombre del usuario del localStorage */
  getUserName(): string | null {
    return localStorage.getItem('userName');
  }

  /** Guarda la URL de la foto del usuario en localStorage */
  saveUserPicture(picture: string | null): void {
    if (picture) {
      localStorage.setItem('userPicture', picture);
    } else {
      localStorage.removeItem('userPicture');
    }
  }

  /** Obtiene la URL de la foto del usuario del localStorage */
  getUserPicture(): string | null {
    return localStorage.getItem('userPicture');
  }
}
