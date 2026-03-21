# Plan de Implementación: Unificación backend → ms-security

## Visión General

Migrar e integrar el módulo `backend` dentro de `ms-security` (paquete `com.sho.ms_security`).
No se crea un proyecto nuevo; solo se modifican/agregan archivos en `ms-security`.
Spring Boot se baja de 4.0.3 a 3.4.5 para compatibilidad con OAuth2 + jjwt 0.11.5.

## Tareas

- [x] 1. Migrar `pom.xml` a Spring Boot 3.4.5 y agregar dependencias
  - Cambiar `<version>` del parent de `4.0.3` a `3.4.5`
  - Agregar `spring-boot-starter-oauth2-client`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-security-oauth2-jose`
  - Agregar `jqwik 1.8.5` en scope test
  - Mantener jjwt 0.11.5, MongoDB, Lombok, spring-boot-starter-mail
  - Reemplazar `spring-boot-starter-webmvc` por `spring-boot-starter-web`
  - Reemplazar artefactos de test `*-webmvc-test` y `*-data-mongodb-test` por `spring-boot-starter-test`
  - _Requisitos: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Actualizar `application.properties` y crear `.env.example`
  - Agregar propiedades OAuth2 Google y GitHub usando variables de entorno (`${GOOGLE_CLIENT_ID}`, etc.)
  - Agregar `server.servlet.session.timeout=30m`, `server.servlet.session.cookie.same-site=lax`
  - Agregar `frontend.url=http://localhost:4200`
  - Crear `.env.example` en la raíz de `ms-security` con todas las variables requeridas
  - _Requisitos: 8.6, 9.7, 14.5, 14.6_

- [x] 3. Ampliar el modelo `User` con campos OAuth2
  - Agregar campos `lastName` (String), `picture` (String), `provider` (String) a `com.sho.ms_security.models.User`
  - Agregar `@Indexed(unique = true)` en el campo `email`
  - Agregar `@Document(collection = "user")` explícito si no existe
  - Agregar constructor `User(String email, String name, String password, String picture)` compatible con `AuthService`
  - _Requisitos: 3.1, 3.2, 3.5_

  - [ ]* 3.1 Test de propiedad P2: Persistencia de campos ampliados del modelo User
    - **Propiedad 2: Round-trip de User con todos los campos nuevos**
    - **Valida: Requisitos 3.1, 3.2**

- [x] 4. Actualizar `UserRepository` con métodos requeridos por `AuthService`
  - Agregar `Optional<User> findByEmail(String email)` (Spring Data lo deriva automáticamente)
  - Agregar `boolean existsByEmail(String email)` (Spring Data lo deriva automáticamente)
  - Mantener `getUserByEmail()` y `searchByNameOrEmail()` existentes
  - _Requisitos: 3.5, 4.1, 5.1_

- [x] 5. Crear DTOs en `com.sho.ms_security.models.dto`
  - Crear `RegisterRequest` con anotaciones `@NotBlank`, `@Size`, `@Email`, `@Pattern` (igual al del `backend`)
  - Crear `LoginRequest` con `@NotBlank` en email y password
  - Crear `LoginResponse` con campos: `message`, `token`, `name`, `email`, `picture`
  - _Requisitos: 4.2, 4.3, 4.4, 4.5, 5.1_

- [x] 6. Crear `PasswordEncoderConfig` en `com.sho.ms_security.configurations`
  - Crear clase `@Configuration` con bean `@Bean PasswordEncoder passwordEncoder()` que retorna `new BCryptPasswordEncoder()`
  - _Requisitos: 2.1, 2.3_

  - [ ]* 6.1 Test de propiedad P1: BCrypt round-trip
    - **Propiedad 1: encode() seguido de matches() debe retornar true para cualquier contraseña**
    - **Valida: Requisitos 2.1, 2.2**

- [x] 7. Actualizar `UserService` para usar BCrypt en lugar de SHA-256
  - Inyectar `PasswordEncoder` (eliminar `EncryptionService`)
  - Reemplazar `theEncryption.convertSHA256(password)` por `passwordEncoder.encode(password)` en `create()` y `update()`
  - _Requisitos: 2.1, 2.5_

- [x] 8. Agregar `sendWelcomeEmail()` a `EmailNotificationService`
  - Agregar método `@Async sendWelcomeEmail(String email, String name)` que llama a `POST {notificationServiceUrl}/send-email` con body `{to, subject, body}`
  - Capturar excepciones y loguear sin relanzar
  - _Requisitos: 4.8, 15.1, 15.2, 15.3, 15.4_

  - [ ]* 8.1 Test de propiedad P23: Resiliencia ante fallo de ms-notificaciones
    - **Propiedad 23: Si ms-notificaciones falla, la operación principal debe completarse sin excepción**
    - **Valida: Requisito 15.3**

- [x] 9. Crear `AuthService` en `com.sho.ms_security.services`
  - Implementar `login(LoginRequest)` → normaliza email, busca por `findByEmail`, verifica con `passwordEncoder.matches()`, genera JWT con `JwtService`, retorna `LoginResponse`
  - Implementar `register(RegisterRequest)` → normaliza email, verifica `existsByEmail`, hashea con BCrypt, establece `provider="LOCAL"`, guarda, asigna rol `CIUDADANO` via `UserRoleRepository`, llama `emailNotificationService.sendWelcomeEmail()` async, retorna `"Registro exitoso"`
  - Implementar `findOrCreateOAuthUser(String email, String name, String picture, String provider)` → busca por email; si existe actualiza name y picture; si no existe crea con provider dado, `password=null`, asigna rol `CIUDADANO`
  - Implementar `changePassword(User user, String oldPassword, String newPassword)` → verifica provider != LOCAL, verifica contraseña actual con BCrypt, valida fortaleza nueva contraseña, actualiza hash
  - Implementar `getUserFromToken(String token)` → delega en `JwtService.getUserFromToken()`
  - _Requisitos: 4.1, 4.6, 4.9, 4.10, 5.1, 5.3, 5.4, 5.5, 7.1, 7.3, 7.4, 7.5, 8.3, 8.4, 9.4, 9.5_

  - [ ]* 9.1 Test de propiedad P4: Registro exitoso crea usuario con BCrypt
    - **Propiedad 4: RegisterRequest válido → usuario en MongoDB con hash BCrypt, HTTP 201, login posterior exitoso**
    - **Valida: Requisitos 4.1, 4.9, 5.1**

  - [ ]* 9.2 Test de propiedad P5: Validación rechaza entradas inválidas
    - **Propiedad 5: Campos fuera de restricciones → HTTP 400, sin usuario creado en BD**
    - **Valida: Requisitos 4.2, 4.3, 4.4, 4.5, 4.7, 7.2**

  - [ ]* 9.3 Test de propiedad P6: Email duplicado retorna 409
    - **Propiedad 6: Email ya registrado → HTTP 409, sin documento duplicado en MongoDB**
    - **Valida: Requisito 4.6**

  - [ ]* 9.4 Test de propiedad P7: Rol CIUDADANO asignado automáticamente
    - **Propiedad 7: Registro exitoso (LOCAL u OAuth2 nuevo) → UserRole con CIUDADANO en MongoDB**
    - **Valida: Requisitos 4.10, 8.3, 9.4**

  - [ ]* 9.5 Test de propiedad P9: Credenciales inválidas retornan 401 genérico
    - **Propiedad 9: Email inexistente o contraseña incorrecta → HTTP 401, mensaje "Credenciales inválidas"**
    - **Valida: Requisitos 5.3, 6.2**

  - [ ]* 9.6 Test de propiedad P10: Normalización de email a minúsculas
    - **Propiedad 10: Email con mayúsculas en registro y login deben tratarse como equivalentes**
    - **Valida: Requisito 5.5**

  - [ ]* 9.7 Test de propiedad P12: Cambio de contraseña round-trip
    - **Propiedad 12: Tras cambio exitoso, login con contraseña antigua falla y con nueva tiene éxito**
    - **Valida: Requisitos 7.1, 7.5**

  - [ ]* 9.8 Test de propiedad P13: Contraseña actual incorrecta bloquea el cambio
    - **Propiedad 13: Contraseña actual incorrecta → HTTP 400, contraseña en BD sin modificar**
    - **Valida: Requisito 7.3**

  - [ ]* 9.9 Test de propiedad P14: Usuarios OAuth2 no pueden cambiar contraseña
    - **Propiedad 14: provider GOOGLE o GITHUB → HTTP 400, mensaje específico**
    - **Valida: Requisito 7.4**

  - [ ]* 9.10 Test de propiedad P15: OAuth2 actualiza usuario existente sin duplicar
    - **Propiedad 15: Email ya en MongoDB + OAuth2 → actualiza name/picture, sin documento duplicado**
    - **Valida: Requisitos 8.4, 9.5**

  - [ ]* 9.11 Test de propiedad P3: Provider correcto según tipo de registro
    - **Propiedad 3: LOCAL → provider="LOCAL"; Google → "GOOGLE"; GitHub → "GITHUB"**
    - **Valida: Requisitos 3.3, 3.4, 8.3, 9.4**

- [x] 10. Crear `CustomOAuth2UserService` en `com.sho.ms_security.configurations`
  - Extender `DefaultOAuth2UserService`
  - En `loadUser()`: detectar proveedor (`google` vs `github`) por `userRequest.getClientRegistration().getRegistrationId()`
  - Para Google: extraer `email`, `name`, `picture`
  - Para GitHub: extraer `login`, `avatar_url`; si `email` es null/blank construir `<login>@github.user`
  - Llamar `authService.findOrCreateOAuthUser(email, name, picture, provider.toUpperCase())`
  - Generar JWT con `jwtService.generateToken(user)` y almacenarlo en el atributo de sesión para el success handler
  - _Requisitos: 8.2, 8.3, 8.4, 9.2, 9.3, 9.4, 9.5_

  - [ ]* 10.1 Test de propiedad P16: Email sintético para GitHub con email privado
    - **Propiedad 16: email null/blank de GitHub → email sintético `<login>@github.user`**
    - **Valida: Requisito 9.3**

- [x] 11. Crear `SecurityConfig` en `com.sho.ms_security.configurations`
  - Definir bean `CorsConfigurationSource` con orígenes `http://localhost:*` y `http://127.0.0.1:*`, métodos GET/POST/PUT/DELETE/OPTIONS
  - Definir `SecurityFilterChain` con:
    - CSRF deshabilitado para `/api/**`
    - Rutas públicas: `POST /api/auth/register`, `POST /api/auth/login`, `/oauth2/**`, `/login/oauth2/**`
    - OAuth2 login con `CustomOAuth2UserService` y success handler que genera JWT y redirige a `${frontend.url}/auth-callback?token=<jwt>`
    - Sesión stateful (necesaria para OAuth2 flow)
  - _Requisitos: 14.1, 14.2, 14.3, 14.5, 14.6_

  - [ ]* 11.1 Test de propiedad P21: Endpoints públicos accesibles sin autenticación
    - **Propiedad 21: /api/auth/register, /api/auth/login, /oauth2/** sin token → no HTTP 401**
    - **Valida: Requisito 14.3**

  - [ ]* 11.2 Test de propiedad P22: Endpoints protegidos requieren token válido
    - **Propiedad 22: /api/** sin exclusión + sin token o token inválido → HTTP 401**
    - **Valida: Requisito 14.4**

- [x] 12. Actualizar `WebConfig` para excluir los nuevos endpoints de auth del interceptor
  - Agregar `.excludePathPatterns("/api/auth/register", "/api/auth/login", "/api/auth/me", "/api/auth/change-password")`
  - Mantener exclusiones existentes (`/api/public/**`, `/api/users/register`)
  - _Requisitos: 14.4_

- [x] 13. Crear `AuthController` en `com.sho.ms_security.controllers`
  - `POST /api/auth/login` → `@Valid LoginRequest`, delega en `authService.login()`, retorna 200 con `LoginResponse` o 401
  - `POST /api/auth/register` → `@Valid RegisterRequest`, delega en `authService.register()`, retorna 201 o 409/400
  - `GET /api/auth/me` → extrae usuario del token via `validatorsService.getUser(request)`, retorna id/name/lastName/email/picture/provider sin password
  - `POST /api/auth/change-password` → extrae usuario del token, delega en `authService.changePassword()`, retorna 200 o 400
  - _Requisitos: 4.1, 4.6, 4.7, 4.9, 5.1, 5.3, 5.4, 6.1, 6.3, 7.1, 7.3, 7.4, 7.5_

  - [ ]* 13.1 Test de propiedad P11: /me retorna datos del usuario sin exponer password
    - **Propiedad 11: GET /api/auth/me con JWT válido → id/name/lastName/email/picture/provider, sin campo password**
    - **Valida: Requisitos 6.1, 6.3**

- [x] 14. Checkpoint — Verificar compilación y tests
  - Asegurar que el proyecto compila sin errores con `mvn compile`
  - Ejecutar `mvn test` y verificar que todos los tests pasan
  - Preguntar al usuario si hay dudas antes de continuar

- [x] 15. Eliminar `EncryptionService` y `SecurityController`
  - Eliminar `com/sho/ms_security/services/EncryptionService.java`
  - Eliminar `com/sho/ms_security/controllers/SecurityController.java`
  - Eliminar `com/sho/ms_security/services/SecurityService.java` (reemplazado por `AuthService`)
  - Verificar que no queden referencias a estas clases en ningún otro archivo
  - _Requisitos: 2.5_

- [ ] 16. Tests de propiedades restantes con jqwik
  - [ ]* 16.1 Test de propiedad P8: JWT contiene los claims correctos
    - **Propiedad 8: generateToken(user) → claims id/name/email; getUserFromToken() recupera los mismos valores**
    - **Valida: Requisito 5.2**

  - [ ]* 16.2 Test de propiedad P17: Eliminación de rol con usuarios asignados retorna 409
    - **Propiedad 17: DELETE /roles/{id} con UserRole existente → HTTP 409, rol no eliminado**
    - **Valida: Requisitos 10.4, 10.5**

  - [ ]* 16.3 Test de propiedad P18: Relaciones usuario-rol y rol-permiso son persistentes
    - **Propiedad 18: POST user-role o role-permission → relación recuperable en consulta posterior**
    - **Valida: Requisitos 12.1, 13.1**

  - [ ]* 16.4 Test de propiedad P19: Permisos de múltiples roles se acumulan
    - **Propiedad 19: Usuario con N roles → permisos efectivos = unión de todos sus roles**
    - **Valida: Requisito 12.2**

  - [ ]* 16.5 Test de propiedad P20: Recurso inexistente retorna 404
    - **Propiedad 20: userId/roleId/permissionId inexistente en asignación → HTTP 404, sin relación huérfana**
    - **Valida: Requisitos 12.4, 13.4**

- [x] 17. Checkpoint final — Verificar integración completa
  - Ejecutar `mvn test` y confirmar que todos los tests pasan
  - Verificar que el proyecto arranca correctamente con `mvn spring-boot:run` (ejecutar manualmente)
  - Preguntar al usuario si hay dudas antes de dar por finalizada la implementación

## Notas

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- El orden de las tareas respeta las dependencias: no se puede crear `AuthService` (tarea 9) antes de `PasswordEncoderConfig` (tarea 6) ni de los DTOs (tarea 5)
- `SecurityService` se elimina en la tarea 15 junto con `EncryptionService`; `AuthService` es su reemplazo completo
- Los tests de propiedades usan jqwik con `@Property(tries = 100)` mínimo y deben incluir el comentario `// Feature: backend-ms-security-unification, Propiedad N: <texto>`
- `ms-notificaciones` (Flask, puerto 5000) no requiere ningún cambio
- Los componentes `SecurityInterceptor`, `ValidatorsService`, `JwtService`, `DataInitializer`, `RoleController`, `PermissionController`, `UserRoleController`, `RolePermissionController`, `SessionController`, `ProfileController` y `UserController` no necesitan modificaciones
