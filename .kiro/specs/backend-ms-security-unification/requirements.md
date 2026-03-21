# Documento de Requisitos

## Introducción

Este documento describe los requisitos para la unificación del módulo `backend` (Spring Boot 3.2, autenticación OAuth2 + email/contraseña) en el módulo `ms-security` (Spring Boot 4.0.3, gestión de roles/permisos/JWT). El resultado es un único microservicio `ms-security` que centraliza toda la autenticación (email/contraseña, Google, GitHub) y la gestión de roles y permisos, manteniendo integración con `ms-notificaciones` (Flask/Python, puerto 5000). La versión de Spring Boot se migra a 3.4.x para garantizar compatibilidad con OAuth2 y jjwt.

---

## Glosario

- **MS_Security**: El microservicio unificado resultante (`ms-security`), ejecutándose en el puerto 8080.
- **MS_Notificaciones**: Microservicio Python/Flask (puerto 5000) que envía correos vía Gmail API.
- **Auth_Service**: Componente interno de MS_Security responsable del registro, login y gestión de sesión con email/contraseña.
- **OAuth2_Service**: Componente interno de MS_Security responsable del flujo OAuth2 con proveedores externos (Google, GitHub).
- **JWT_Service**: Componente interno de MS_Security que genera y valida tokens JWT firmados con HS512 (jjwt 0.11.5).
- **Role_Service**: Componente interno de MS_Security que gestiona roles del sistema.
- **Permission_Service**: Componente interno de MS_Security que gestiona permisos granulares por URL y método HTTP.
- **Security_Interceptor**: Componente interno de MS_Security que valida el token JWT y los permisos del usuario en cada request.
- **Data_Initializer**: Componente interno de MS_Security que crea roles y permisos predeterminados al arrancar.
- **BCrypt_Encoder**: Componente de hashing de contraseñas basado en BCrypt (reemplaza SHA-256).
- **Usuario**: Persona registrada en el sistema con campos: id, email, name, lastName, password (BCrypt), picture, provider (LOCAL/GOOGLE/GITHUB).
- **Rol**: Agrupación de permisos asignada a uno o más usuarios (ej. ADMINISTRADOR_SISTEMA, CONDUCTOR).
- **Permiso**: Combinación de URL + método HTTP que define una acción autorizada en el sistema.
- **Token_JWT**: Token firmado con HS512 que contiene id, name y email del usuario, con expiración configurable.
- **Provider**: Origen del registro del usuario: LOCAL (email/contraseña), GOOGLE o GITHUB.

---

## Requisitos

### Requisito 1: Migración de versión y dependencias

**User Story:** Como desarrollador, quiero que ms-security use Spring Boot 3.4.x con todas las dependencias necesarias, para que OAuth2, jjwt y BCrypt funcionen sin conflictos de versión.

#### Criterios de Aceptación

1. THE MS_Security SHALL usar Spring Boot 3.4.x como versión base del proyecto.
2. THE MS_Security SHALL incluir las dependencias `spring-boot-starter-oauth2-client`, `spring-boot-starter-security`, `spring-boot-starter-validation` y `spring-security-oauth2-jose` en el `pom.xml`.
3. THE MS_Security SHALL mantener las dependencias existentes de jjwt 0.11.5, MongoDB, Lombok y `spring-boot-starter-mail`.
4. THE MS_Security SHALL incluir la dependencia de BCrypt a través de `spring-security-crypto` (incluida en `spring-boot-starter-security`).
5. THE MS_Security SHALL compilar y arrancar sin errores con Java 17.

---

### Requisito 2: Migración de hashing de contraseñas a BCrypt

**User Story:** Como administrador de seguridad, quiero que las contraseñas se almacenen con BCrypt en lugar de SHA-256, para cumplir con estándares modernos de seguridad.

#### Criterios de Aceptación

1. THE BCrypt_Encoder SHALL hashear las contraseñas nuevas usando `BCryptPasswordEncoder` con factor de coste por defecto (10).
2. WHEN un usuario intenta iniciar sesión, THE Auth_Service SHALL verificar la contraseña usando `BCryptPasswordEncoder.matches()`.
3. THE MS_Security SHALL exponer el bean `PasswordEncoder` de tipo `BCryptPasswordEncoder` en el contexto de Spring.
4. IF un usuario registrado con SHA-256 intenta iniciar sesión, THEN THE Auth_Service SHALL retornar un error de credenciales inválidas con mensaje genérico, sin revelar el motivo específico.
5. THE EncryptionService (SHA-256) SHALL ser eliminado del proyecto una vez completada la migración.

---

### Requisito 3: Ampliación del modelo de Usuario

**User Story:** Como desarrollador, quiero que el modelo User de ms-security incluya todos los campos necesarios para soportar OAuth2 y registro completo, para no perder información del usuario.

#### Criterios de Aceptación

1. THE MS_Security SHALL ampliar el documento `User` en MongoDB con los campos: `lastName` (String), `picture` (String, URL de foto de perfil), `provider` (String: LOCAL/GOOGLE/GITHUB/MICROSOFT).
2. THE MS_Security SHALL mantener los campos existentes: `id`, `name`, `email`, `password`.
3. WHEN un usuario se registra con email/contraseña, THE Auth_Service SHALL establecer `provider = "LOCAL"` y `picture = null`.
4. WHEN un usuario se autentica vía OAuth2, THE OAuth2_Service SHALL establecer el `provider` correspondiente al proveedor utilizado.
5. THE MS_Security SHALL indexar el campo `email` como único en MongoDB para prevenir duplicados.

---

### Requisito 4: Registro con email y contraseña (HU-ENTR-1-007)

**User Story:** Como ciudadano, quiero registrarme con mi email y contraseña, para acceder al sistema sin necesidad de una cuenta de terceros.

#### Criterios de Aceptación

1. WHEN se recibe una solicitud POST a `/api/auth/register` con nombre, apellido, email y contraseña válidos, THE Auth_Service SHALL crear el usuario en MongoDB con la contraseña hasheada con BCrypt.
2. THE MS_Security SHALL validar que el campo `name` no esté vacío y tenga entre 2 y 50 caracteres.
3. THE MS_Security SHALL validar que el campo `lastName` no esté vacío y tenga entre 2 y 50 caracteres.
4. THE MS_Security SHALL validar que el campo `email` tenga formato de correo electrónico válido (RFC 5322).
5. THE MS_Security SHALL validar que la contraseña tenga mínimo 8 caracteres, al menos una mayúscula, una minúscula, un dígito y un carácter especial.
6. IF el email ya existe en la base de datos, THEN THE Auth_Service SHALL retornar HTTP 409 con el mensaje `"Ya existe una cuenta con ese email"`.
7. IF alguna validación falla, THEN THE Auth_Service SHALL retornar HTTP 400 con los mensajes de error correspondientes.
8. WHEN el registro es exitoso, THE MS_Security SHALL llamar a MS_Notificaciones en `/send-email` de forma asíncrona para enviar un correo de bienvenida al email registrado.
9. WHEN el registro es exitoso, THE Auth_Service SHALL retornar HTTP 201 con el mensaje `"Registro exitoso"`.
10. THE MS_Security SHALL asignar automáticamente el rol `CIUDADANO` al nuevo usuario registrado con email/contraseña.

---

### Requisito 5: Inicio de sesión con email y contraseña (HU-ENTR-1-008)

**User Story:** Como usuario registrado, quiero iniciar sesión con mi email y contraseña, para obtener un token JWT y acceder al sistema.

#### Criterios de Aceptación

1. WHEN se recibe una solicitud POST a `/api/auth/login` con email y contraseña válidos, THE Auth_Service SHALL verificar las credenciales contra MongoDB y retornar un Token_JWT firmado.
2. WHEN el login es exitoso, THE JWT_Service SHALL generar un token con los claims: `id`, `name`, `email`, firmado con HS512 y con expiración configurable vía `jwt.expiration`.
3. IF el email no existe o la contraseña es incorrecta, THEN THE Auth_Service SHALL retornar HTTP 401 con el mensaje genérico `"Credenciales inválidas"`, sin indicar cuál campo es incorrecto.
4. WHEN el login es exitoso, THE Auth_Service SHALL retornar HTTP 200 con el token JWT, nombre, email y picture del usuario.
5. THE MS_Security SHALL normalizar el email a minúsculas antes de buscar en la base de datos.

---

### Requisito 6: Endpoint de perfil autenticado

**User Story:** Como usuario autenticado, quiero consultar mis datos de perfil, para verificar la información de mi cuenta.

#### Criterios de Aceptación

1. WHEN se recibe una solicitud GET a `/api/auth/me` con un Token_JWT válido en el header `Authorization: Bearer <token>`, THE Auth_Service SHALL retornar los datos del usuario: id, name, lastName, email, picture, provider.
2. IF el token es inválido o ha expirado, THEN THE Security_Interceptor SHALL retornar HTTP 401 con el mensaje `"No autorizado: token inválido o sin permisos"`.
3. THE MS_Security SHALL excluir el campo `password` de la respuesta del endpoint `/api/auth/me`.

---

### Requisito 7: Cambio de contraseña

**User Story:** Como usuario registrado con email/contraseña, quiero cambiar mi contraseña, para mantener la seguridad de mi cuenta.

#### Criterios de Aceptación

1. WHEN se recibe una solicitud POST a `/api/auth/change-password` con token válido, contraseña actual y nueva contraseña, THE Auth_Service SHALL verificar la contraseña actual con BCrypt y actualizar a la nueva contraseña hasheada.
2. THE MS_Security SHALL validar que la nueva contraseña cumpla los mismos requisitos de fortaleza del Requisito 4 (criterio 5).
3. IF la contraseña actual no coincide, THEN THE Auth_Service SHALL retornar HTTP 400 con el mensaje `"Contraseña actual incorrecta"`.
4. IF el usuario se registró vía OAuth2 (provider != LOCAL), THEN THE Auth_Service SHALL retornar HTTP 400 con el mensaje `"Los usuarios OAuth2 no pueden cambiar contraseña desde aquí"`.
5. WHEN el cambio de contraseña es exitoso, THE Auth_Service SHALL retornar HTTP 200 con el mensaje `"Contraseña actualizada. Debe iniciar sesión de nuevo."`.

---

### Requisito 8: Autenticación con Google (HU-ENTR-1-004)

**User Story:** Como ciudadano, quiero iniciar sesión con mi cuenta de Google, para acceder al sistema sin crear una contraseña adicional.

#### Criterios de Aceptación

1. WHEN un usuario accede a `/oauth2/authorization/google`, THE MS_Security SHALL redirigir al flujo de autorización de Google OAuth2.
2. WHEN Google retorna el callback con código de autorización válido, THE OAuth2_Service SHALL obtener el email, nombre y foto de perfil del usuario desde los atributos OAuth2.
3. WHEN el email del usuario de Google no existe en MongoDB, THE OAuth2_Service SHALL crear un nuevo usuario con `provider = "GOOGLE"`, `password = null` y asignar el rol `CIUDADANO`.
4. WHEN el email del usuario de Google ya existe en MongoDB, THE OAuth2_Service SHALL actualizar el nombre y la foto de perfil del usuario existente.
5. WHEN la autenticación con Google es exitosa, THE MS_Security SHALL generar un Token_JWT y redirigir al frontend con el token como parámetro de query (`?token=<jwt>`).
6. THE MS_Security SHALL configurar el `redirect-uri` de Google como `http://localhost:8080/login/oauth2/code/google`.

---

### Requisito 9: Autenticación con GitHub (HU-ENTR-1-006)

**User Story:** Como desarrollador, quiero iniciar sesión con mi cuenta de GitHub, para acceder al sistema usando mis credenciales existentes.

#### Criterios de Aceptación

1. WHEN un usuario accede a `/oauth2/authorization/github`, THE MS_Security SHALL redirigir al flujo de autorización de GitHub OAuth2.
2. WHEN GitHub retorna el callback con código de autorización válido, THE OAuth2_Service SHALL obtener el email, nombre de usuario (`login`) y foto de perfil (`avatar_url`) desde los atributos OAuth2.
3. IF el email retornado por GitHub es nulo o vacío (cuenta con email privado), THEN THE OAuth2_Service SHALL construir un email sintético con el formato `<login>@github.user`.
4. WHEN el email del usuario de GitHub no existe en MongoDB, THE OAuth2_Service SHALL crear un nuevo usuario con `provider = "GITHUB"`, `password = null` y asignar el rol `CIUDADANO`.
5. WHEN el email del usuario de GitHub ya existe en MongoDB, THE OAuth2_Service SHALL actualizar el nombre y la foto de perfil del usuario existente.
6. WHEN la autenticación con GitHub es exitosa, THE MS_Security SHALL generar un Token_JWT y redirigir al frontend con el token como parámetro de query (`?token=<jwt>`).
7. THE MS_Security SHALL configurar el scope de GitHub como `user:email,read:user`.

---

### Requisito 10: Creación y gestión de roles del sistema (HU-ENTR-1-001)

**User Story:** Como administrador del sistema, quiero gestionar los roles del sistema con permisos granulares, para controlar el acceso de cada tipo de usuario.

#### Criterios de Aceptación

1. WHEN MS_Security arranca por primera vez, THE Data_Initializer SHALL crear los roles predeterminados: `ADMINISTRADOR_SISTEMA`, `ADMINISTRADOR_EMPRESA`, `SUPERVISOR`, `CONDUCTOR`, `CIUDADANO`, si no existen.
2. WHEN MS_Security arranca por primera vez, THE Data_Initializer SHALL crear los permisos granulares predeterminados por módulo (usuarios, roles, permisos, buses, rutas, programaciones, reportes, incidentes, mensajes), si no existen.
3. WHEN MS_Security arranca, THE Data_Initializer SHALL asignar todos los permisos al rol `ADMINISTRADOR_SISTEMA`, si las relaciones no existen.
4. WHEN se recibe una solicitud DELETE a `/roles/{id}`, THE Role_Service SHALL verificar que no existan usuarios con ese rol asignado antes de eliminarlo.
5. IF existen usuarios con el rol a eliminar, THEN THE Role_Service SHALL retornar HTTP 409 con el mensaje `"No se puede eliminar el rol: tiene usuarios asignados"`.
6. WHEN se crea o modifica un rol, THE MS_Security SHALL reflejar los cambios inmediatamente en las validaciones del Security_Interceptor sin necesidad de reiniciar.

---

### Requisito 12: Asignación de roles a usuarios (HU-ENTR-1-002)

**User Story:** Como administrador del sistema, quiero asignar múltiples roles a un usuario, para que acumule los permisos de todos sus roles.

#### Criterios de Aceptación

1. WHEN se recibe una solicitud POST a `/user-role/user/{userId}/role/{roleId}`, THE Role_Service SHALL crear la relación usuario-rol en MongoDB.
2. WHILE un usuario tiene múltiples roles asignados, THE Security_Interceptor SHALL acumular los permisos de todos los roles para evaluar el acceso.
3. WHEN se asigna o revoca un rol a un usuario, THE MS_Security SHALL llamar a MS_Notificaciones en `/send-role-change` de forma asíncrona con el email del usuario, nombre, nombre del rol y la acción (`"Asignación"` o `"Revocación"`).
4. IF el usuario o el rol no existen, THEN THE Role_Service SHALL retornar HTTP 404 con el mensaje correspondiente.
5. WHEN se elimina una relación usuario-rol (DELETE `/user-role/{id}`), THE MS_Security SHALL reflejar la pérdida de permisos inmediatamente en el siguiente request del usuario.

---

### Requisito 13: Gestión de permisos específicos por rol (HU-ENTR-1-003)

**User Story:** Como administrador del sistema, quiero asignar y quitar permisos específicos a un rol, para controlar con granularidad qué acciones puede realizar cada rol.

#### Criterios de Aceptación

1. WHEN se recibe una solicitud POST a `/role-permission/role/{roleId}/permission/{permissionId}`, THE Permission_Service SHALL crear la relación rol-permiso en MongoDB.
2. WHEN se elimina una relación rol-permiso (DELETE `/role-permission/{id}`), THE Security_Interceptor SHALL reflejar el cambio inmediatamente en el siguiente request, sin necesidad de reiniciar el servicio.
3. WHEN se modifica un permiso de un rol, THE MS_Security SHALL llamar a MS_Notificaciones en `/send-permission-change` de forma asíncrona para notificar a todos los usuarios que tienen ese rol asignado.
4. IF el rol o el permiso no existen, THEN THE Permission_Service SHALL retornar HTTP 404 con el mensaje correspondiente.
5. THE Security_Interceptor SHALL consultar la base de datos en cada request para evaluar permisos, garantizando que los cambios apliquen de forma inmediata.

---

### Requisito 14: Configuración de seguridad y CORS

**User Story:** Como desarrollador, quiero que ms-security tenga una configuración de seguridad correcta que permita OAuth2 y JWT simultáneamente, para que el frontend Angular pueda consumir la API sin problemas de CORS.

#### Criterios de Aceptación

1. THE MS_Security SHALL configurar CORS para permitir orígenes `http://localhost:*` y `http://127.0.0.1:*` con métodos GET, POST, PUT, DELETE, OPTIONS.
2. THE MS_Security SHALL deshabilitar CSRF para los endpoints REST (`/api/**`).
3. THE MS_Security SHALL exponer como públicos (sin autenticación) los endpoints: `POST /api/auth/register`, `POST /api/auth/login`, `GET /oauth2/authorization/**`, `GET /login/oauth2/code/**`.
4. THE MS_Security SHALL proteger con el Security_Interceptor todos los endpoints bajo `/api/**`, excluyendo `/api/auth/register`, `/api/auth/login` y `/api/public/**`.
5. THE MS_Security SHALL configurar el `defaultSuccessUrl` del flujo OAuth2 para redirigir al frontend Angular en `http://localhost:4200/auth-callback`.
6. THE MS_Security SHALL mantener la configuración de sesión con `session.timeout=30m` y `cookie.same-site=lax` para el flujo OAuth2.

---

### Requisito 15: Integración con ms-notificaciones

**User Story:** Como sistema, quiero que ms-security llame correctamente a ms-notificaciones para enviar emails, para que los usuarios reciban notificaciones de eventos importantes.

#### Criterios de Aceptación

1. THE MS_Security SHALL configurar la URL de ms-notificaciones vía la propiedad `notification.service.url` (default: `http://localhost:5000`).
2. WHEN se llama a MS_Notificaciones, THE MS_Security SHALL realizar la llamada de forma asíncrona (usando `@Async`) para no bloquear el hilo principal.
3. IF MS_Notificaciones no está disponible, THEN THE MS_Security SHALL registrar el error en el log y continuar la operación principal sin lanzar excepción al cliente.
4. THE MS_Security SHALL enviar correo de bienvenida vía `/send-email` al registrarse un nuevo usuario (HU-ENTR-1-007).
5. THE MS_Security SHALL enviar notificación de cambio de rol vía `/send-role-change` al asignar o revocar un rol (HU-ENTR-1-002).
6. THE MS_Security SHALL enviar notificación de cambio de permisos vía `/send-permission-change` al modificar permisos de un rol (HU-ENTR-1-003).
