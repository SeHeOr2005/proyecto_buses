# Sistema de Autenticación - Full Stack

Proyecto con Angular (frontend) y Spring Boot (backend) para autenticación mínima funcional.

## Estructura

```
proyecto_buses/
├── backend/          # Spring Boot API
└── frontend/         # Angular SPA
```

## Usuario de prueba

- **Email:** admin@test.com
- **Contraseña:** 123456

---

## Cómo ejecutar

### 1. Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run
```

O con Maven instalado:

```bash
cd backend
mvn spring-boot:run
```

El backend corre en: **http://localhost:8080**

### 2. Frontend (Angular)

```bash
cd frontend
npm install
npm start
```

El frontend corre en: **http://localhost:4200**

---

## Probar el login

1. Iniciar primero el **backend** y luego el **frontend**.
2. Abrir http://localhost:4200 en el navegador.
3. Ingresar las credenciales:
   - Email: `admin@test.com`
   - Contraseña: `123456`
4. Al hacer clic en "Iniciar sesión" deberías ser redirigido a `/home`.
5. En la página Home verás "HOLA, SESIÓN INICIADA" y tu email.

---

## Endpoints del API

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | /api/auth/login | Login con email y password |

**Request:**
```json
{
  "email": "admin@test.com",
  "password": "123456"
}
```

**Response exitoso (200):**
```json
{
  "message": "Login exitoso",
  "token": "fake-jwt-token"
}
```

**Response error (401):**
```
Email o contraseña incorrectos
```
