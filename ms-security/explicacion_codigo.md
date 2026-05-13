# 📖 Explicación del Código de `ms-security` — Archivo por Archivo

Este documento explica **cada línea importante** de todo el código del microservicio, organizado por capas (de abajo hacia arriba).

---

## 📐 Estructura del Proyecto

```
ms-security/
├── src/main/java/com/sho/ms_security/
│   ├── MsSecurityApplication.java          ← Punto de entrada
│   ├── configurations/
│   │   ├── DataInitializer.java            ← Carga datos iniciales al arrancar
│   │   └── WebConfig.java                  ← Configura qué rutas proteger
│   ├── interceptors/
│   │   └── SecurityInterceptor.java        ← "Guardia" que revisa cada petición
│   ├── models/                             ← Entidades (documentos MongoDB)
│   │   ├── User.java, Role.java, Permission.java, Session.java
│   │   ├── Profile.java, UserRole.java, RolePermission.java
│   │   └── LoginRequest.java, OAuthLoginRequest.java, etc. (DTOs)
│   ├── repositories/                       ← Acceso a MongoDB
│   │   ├── UserRepository.java, RoleRepository.java, etc.
│   │   └── SessionRepository.java
│   ├── services/                           ← Lógica de negocio
│   │   ├── SecurityService.java            ← Login, 2FA, OAuth, sesiones
│   │   ├── JwtService.java                 ← Generar/validar tokens JWT
│   │   ├── EncryptionService.java          ← Cifrar contraseñas (SHA-256)
│   │   ├── FirebaseAuthService.java        ← OAuth con Google/Microsoft/GitHub
│   │   ├── RecaptchaVerificationService.java ← Protección anti-bots
│   │   ├── ValidatorsService.java          ← Verifica permisos en cada request
│   │   └── UserService.java, RoleService.java, etc.
│   └── controllers/                        ← Endpoints REST
│       ├── SecurityController.java         ← /security/* (login, 2FA, OAuth)
│       ├── UserController.java             ← /api/users/*
│       └── RoleController.java, etc.
└── src/main/resources/
    └── application.properties              ← Configuración
```

---

## 1️⃣ Punto de Entrada — [MsSecurityApplication.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/MsSecurityApplication.java)

```java
@SpringBootApplication
@EnableAsync
public class MsSecurityApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsSecurityApplication.class, args);
    }
}
```

| Anotación | ¿Qué hace? |
|-----------|------------|
| `@SpringBootApplication` | Le dice a Spring: "esta es la clase principal, escanea todo el paquete `com.sho.ms_security` y configura todo automáticamente" |
| `@EnableAsync` | Habilita métodos asíncronos (los que tienen `@Async`). Sin esto, los emails se enviarían de forma **síncrona** y bloquearían la respuesta al usuario |

> [!TIP]
> Gracias a `@EnableAsync`, cuando el sistema envía un email (ej: notificación de cambio de rol), **no hace esperar** al usuario. El email se envía en un hilo aparte.

---

## 2️⃣ Modelos (Entidades) — Carpeta `models/`

### 📄 [User.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/models/User.java) — El usuario del sistema

```java
@Data                    // Lombok: genera automáticamente getters, setters, toString, equals, hashCode
@Document                // Spring Data MongoDB: esta clase se guardará como documento en MongoDB
public class User {
    @Id                  // Este campo es la clave primaria (_id en MongoDB)
    private String id;
    
    private String name;
    private String email;
    private String password;           // Se guarda como hash SHA-256, NUNCA en texto plano
    private String avatar;             // Foto (URL o data:image/... en base64)
    
    // --- Campos para OAuth (login social) ---
    private String firebaseUid;        // ID único del usuario en Firebase
    private String previousFirebaseUid;// UID anterior si desvinculó su cuenta social
    private String authProvider;       // "google.com", "microsoft.com", "github.com"
    private Boolean emailVerified;     // ¿Firebase verificó su email?
    
    private Boolean active = true;     // Si es false, el usuario está "eliminado lógicamente"
    private Date lastLoginAt;          // Último login (se actualiza en cada OAuth login)
    
    @Transient                         // @Transient = NO se guarda en MongoDB, solo existe en memoria
    private Boolean unlinkSocialAccount; // Flag temporal: el frontend lo envía cuando quiere desvincular OAuth
    
    // --- Campos para recuperación de contraseña ---
    private String resetPasswordToken;          // Token UUID para el link del email
    private Date resetPasswordTokenExpiration;  // Cuándo expira ese token
}
```

**¿Qué es `@Transient`?** El campo `unlinkSocialAccount` **no se guarda nunca en la base de datos**. Solo sirve para que el frontend le diga al backend "quiero desvincular mi cuenta de Google". El backend lo lee, hace su trabajo, y lo descarta.

### 📄 [Role.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/models/Role.java) — Roles del sistema

```java
@Data
@Document
public class Role {
    @Id
    private String id;
    private String name;         // Ej: "ADMINISTRADOR_SISTEMA", "CONDUCTOR", "CIUDADANO"
    private String description;  // Ej: "Conductor de unidad de transporte"
}
```

Muy sencillo: un rol solo tiene nombre y descripción. La "magia" está en cómo se conecta con usuarios y permisos.

### 📄 [Permission.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/models/Permission.java) — Permisos granulares

```java
@Data
@Document
public class Permission {
    @Id
    private String id;
    private String url;    // Ej: "/api/users", "/api/buses/?"
    private String method; // "GET", "POST", "PUT", "DELETE"
    private String model;  // Módulo al que pertenece: "usuarios", "buses", "rutas"
}
```

Cada permiso representa **una acción específica**. Por ejemplo:
- `{url: "/api/buses", method: "GET", model: "buses"}` → "poder ver la lista de buses"
- `{url: "/api/buses/?", method: "DELETE", model: "buses"}` → "poder eliminar un bus"

### 📄 [Session.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/models/Session.java) — Sesiones activas

```java
@Data
@Document
public class Session {
    @Id
    private String id;
    private String token;                    // El JWT completo (o "2fa_uuid" si es pendiente)
    private String jti;                      // ID único del JWT (para rastreo)
    private Date expiration;                 // Cuándo expira la sesión
    
    // --- Campos para 2FA ---
    private String code2FA;                  // Código de 6 dígitos (ej: "483729")
    private Integer twoFactorAttemptsLeft;   // Intentos restantes (empieza en 3)
    private Date twoFactorVerifiedAt;        // Cuándo se verificó el código
    
    private String provider;                 // "password", "google.com", etc.
    private String deviceInfo;               // Info del dispositivo (no usado actualmente)
    private String ip;                       // IP del cliente (no usado actualmente)
    private Date revokedAt;                  // Si no es null → sesión cerrada/revocada

    @DBRef                                   // Referencia a otro documento (como FK en SQL)
    private User user;                       // El usuario dueño de esta sesión
}
```

**¿Qué es `@DBRef`?** En MongoDB no hay "foreign keys" como en SQL. `@DBRef` le dice a Spring: "este campo es una **referencia** a un documento User. Cuando cargues esta sesión, ve a la colección `user` y trae el documento completo".

**¿Cómo funciona una sesión pendiente de 2FA?**
- Se crea con `token = "2fa_uuid-random"`, `code2FA = "483729"`, `twoFactorAttemptsLeft = 3`
- Si el usuario acierta el código → el `token` se reemplaza por el JWT real, `code2FA` se pone en `null`
- Si se agotan los intentos → `revokedAt` se establece y la sesión queda inutilizable

### 📄 [UserRole.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/models/UserRole.java) — "Juan ES Conductor"

```java
@Data
@Document
public class UserRole {
    @Id
    private String id;

    @DBRef
    private User user;   // Referencia al usuario

    @DBRef
    private Role role;   // Referencia al rol
}
```

Esta es una **tabla pivote** (tabla de muchos a muchos). Cada documento dice "este usuario tiene este rol". Un usuario puede tener **múltiples** UserRole, o sea, múltiples roles.

### 📄 [RolePermission.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/models/RolePermission.java) — "Conductor PUEDE ver buses"

```java
@Data
@Document
public class RolePermission {
    @Id
    private String id;

    @DBRef
    private Role role;           // Referencia al rol

    @DBRef
    private Permission permission; // Referencia al permiso
}
```

Otra **tabla pivote**. Cada documento dice "este rol tiene este permiso".

### 📄 DTOs de Request — "Envoltorios" para los datos del frontend

Estos son **DTOs** (Data Transfer Objects): clases simples que solo transportan datos del frontend al backend.

````carousel
**LoginRequest.java** — Para el login con contraseña
```java
@Data
public class LoginRequest {
    private String email;
    private String password;
    private String recaptchaToken;  // Token de reCAPTCHA del frontend
}
```
<!-- slide -->
**OAuthLoginRequest.java** — Para el login con Google/Microsoft/GitHub
```java
@Data
public class OAuthLoginRequest {
    private String firebaseIdToken;  // Token que Firebase le da al frontend
    private String avatarUrl;        // URL de la foto de perfil del proveedor
}
```
<!-- slide -->
**TwoFactorVerifyRequest.java** — Para verificar el código 2FA
```java
@Data
public class TwoFactorVerifyRequest {
    private String challengeToken;  // El token temporal "2fa_uuid..."
    private String code;            // El código de 6 dígitos que el usuario escribió
}
```
<!-- slide -->
**PasswordRecoveryRequest.java** — Para pedir recuperación de contraseña
```java
@Data
public class PasswordRecoveryRequest {
    private String email;
    private String recaptchaToken;
}
```
<!-- slide -->
**ResetPasswordRequest.java** — Para establecer la nueva contraseña
```java
@Data
public class ResetPasswordRequest {
    private String token;           // Token UUID que llegó en el email
    private String newPassword;     // La nueva contraseña
    private String recaptchaToken;
}
```
````

---

## 3️⃣ Repositorios — Carpeta `repositories/`

Los repositorios son **interfaces** que Spring implementa automáticamente. Hacen las consultas a MongoDB.

### 📄 [UserRepository.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/repositories/UserRepository.java)

```java
public interface UserRepository extends MongoRepository<User, String> {
    // MongoRepository ya da gratis: findAll(), findById(), save(), delete(), existsById()

    @Query("{'email': ?0}")  // ?0 = primer parámetro del método
    User getUserByEmail(String email);

    @Query("{'firebaseUid': ?0}")
    User getUserByFirebaseUid(String firebaseUid);

    @Query("{'previousFirebaseUid': ?0}")
    User getUserByPreviousFirebaseUid(String previousFirebaseUid);

    @Query("{'resetPasswordToken': ?0}")
    User getUserByResetPasswordToken(String resetPasswordToken);

    // Búsqueda fuzzy: busca en name O email, ignorando mayúsculas/minúsculas
    @Query("{ $or: [ {'name': {$regex: ?0, $options: 'i'}}, {'email': {$regex: ?0, $options: 'i'}} ] }")
    List<User> searchByNameOrEmail(String query);
}
```

**¿Cómo funciona `@Query`?** Son consultas de MongoDB en formato JSON. El `?0` se reemplaza por el primer parámetro, `?1` por el segundo, etc.

### 📄 [SessionRepository.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/repositories/SessionRepository.java)

```java
public interface SessionRepository extends MongoRepository<Session, String> {
    // Busca una sesión activa (no revocada) por su token
    @Query("{'token': ?0, 'revokedAt': null}")
    Session findActiveByToken(String token);

    // Busca una sesión pendiente de 2FA (tiene código 2FA y no está revocada)
    @Query("{'token': ?0, 'revokedAt': null, 'code2FA': {$ne: null}}")
    Session findPendingTwoFactorByToken(String token);
}
```

**Clave**: `'revokedAt': null` significa "solo sesiones que NO han sido revocadas". `$ne: null` significa "not equal to null" (que el campo code2FA tenga algún valor).

### 📄 [RolePermissionRepository.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/repositories/RolePermissionRepository.java)

```java
public interface RolePermissionRepository extends MongoRepository<RolePermission, String> {
    // Obtiene todos los permisos de un rol específico
    @Query("{ 'role' : { '$ref' : 'role', '$id' : { '$oid': ?0 } } }")
    List<RolePermission> getPermissionsByRole(String roleId);

    // Verifica si existe una combinación específica rol+permiso
    @Query("{ 'role' : { '$ref' : 'role', '$id' : { '$oid': ?0 } }, 'permission' : { '$ref' : 'permission', '$id' : { '$oid': ?1 } } }")
    RolePermission getRolePermission(String roleId, String permissionId);
}
```

**¿Por qué esa sintaxis tan rara con `$ref` y `$oid`?** Porque usamos `@DBRef`. MongoDB almacena las referencias como `{ "$ref": "nombre_coleccion", "$id": ObjectId("...") }`. Para buscar por esos campos, hay que usar esa sintaxis específica.

### 📄 [UserRoleRepository.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/repositories/UserRoleRepository.java)

```java
public interface UserRoleRepository extends MongoRepository<UserRole, String> {
    // Obtener todos los roles de un usuario
    @Query("{ 'user' : { '$ref' : 'user', '$id' : { '$oid': ?0 } } }")
    List<UserRole> getRolesByUser(String userId);

    // Obtener todos los usuarios que tienen un rol (para validar eliminación de rol)
    @Query("{ 'role' : { '$ref' : 'role', '$id' : { '$oid': ?0 } } }")
    List<UserRole> getUsersByRole(String roleId);
}
```

### 📄 Repositorios simples

```java
// PermissionRepository.java — Busca un permiso por URL + método
public interface PermissionRepository extends MongoRepository<Permission, String> {
    @Query("{'url': ?0, 'method': ?1}")
    Permission getPermission(String url, String method);
}

// RoleRepository.java — Busca un rol por nombre
public interface RoleRepository extends MongoRepository<Role, String> {
    @Query("{'name': ?0}")
    Role findByName(String name);
}

// ProfileRepository.java — Sin queries personalizadas, solo hereda las básicas
public interface ProfileRepository extends MongoRepository<Profile, String> {}
```

---

## 4️⃣ Servicios — Carpeta `services/`

### 📄 [EncryptionService.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/services/EncryptionService.java) — Cifrado de contraseñas

```java
@Service
public class EncryptionService {

    public String convertSHA256(String password) {
        MessageDigest md = MessageDigest.getInstance("SHA-256"); // Algoritmo de hash
        byte[] hash = md.digest(password.getBytes());            // Convierte el texto a bytes cifrados
        StringBuffer sb = new StringBuffer();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));  // Convierte cada byte a hexadecimal
        }
        return sb.toString();  // Resultado: "a5b9f3c2..." (64 caracteres)
    }
}
```

**Ejemplo real**:
- `"MiPassword123!"` → `"8f14e45fceea167a5a36dedd4bea2543..."` (siempre el mismo resultado)
- Es **unidireccional**: no se puede revertir el hash para obtener la contraseña original

---

### 📄 [JwtService.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/services/JwtService.java) — Tokens JWT

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")      // Inyecta el valor de application.properties
    private String secret;

    @Value("${jwt.expiration}")  // 3600000 ms = 1 hora
    private Long expiration;

    // Genera la clave de firma a partir del secreto configurado
    private Key getSecretKey() {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
        // La clave "aaa" se transforma a un hash SHA-512 de 64 bytes
        // Eso garantiza que la clave siempre tenga el tamaño adecuado para HMAC-SHA512
    }
```

**¿Por qué se hace hash de la clave?** Porque HMAC-SHA512 necesita una clave de al menos 64 bytes. Si alguien pone `jwt.secret=aaa` (como en tu application.properties), el SHA-512 la convierte en 64 bytes seguros.

```java
    // Genera un nuevo token JWT para un usuario
    public String generateToken(User theUser) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);  // Ahora + 1 hora

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", theUser.getId());        // ID del usuario
        claims.put("name", theUser.getName());    // Nombre
        claims.put("email", theUser.getEmail());  // Email
        claims.put("jti", UUID.randomUUID().toString());  // ID único para rastrear este token

        return Jwts.builder()
                .setClaims(claims)                // Datos del usuario dentro del token
                .setSubject(theUser.getName())     // Subject (estándar JWT)
                .setIssuedAt(now)                  // Fecha de creación
                .setExpiration(expiryDate)         // Fecha de expiración
                .signWith(getSecretKey())          // Firma digital con la clave secreta
                .compact();                        // Genera el string JWT final
        // El resultado es algo como: "eyJhbGciOiJIUzUxMiJ9.eyJpZCI6ImFiYy..."
    }
```

```java
    // Valida si un token JWT es legítimo y no ha expirado
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())   // Usa la misma clave para verificar la firma
                    .build()
                    .parseClaimsJws(token);           // Si la firma no coincide → excepción
            Date now = new Date();
            return !claimsJws.getBody().getExpiration().before(now);  // ¿Aún no ha expirado?
        } catch (Exception e) {
            return false;  // Token inválido, manipulado, o expirado
        }
    }

    // Extrae los datos del usuario desde un token JWT
    public User getUserFromToken(String token) {
        // Parsea el token → extrae los claims → crea un objeto User con esos datos
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        User user = new User();
        user.setId((String) claims.get("id"));
        user.setName((String) claims.get("name"));
        user.setEmail((String) claims.get("email"));
        return user;
    }

    // Obtiene el identificador único (jti) de un token
    public String getTokenId(String token) {
        // Útil para rastrear sesiones específicas
        return (String) Jwts.parserBuilder()...parseClaimsJws(token).getBody().get("jti");
    }
}
```

---

### 📄 [SecurityService.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/services/SecurityService.java) — El cerebro del sistema

Este es el servicio **más importante y complejo**. Maneja toda la lógica de autenticación.

#### Método `login()` — Login con contraseña

```java
public Map<String, Object> login(User theNewUser) {
    // 1. Buscar al usuario por email en la BD
    User theActualUser = this.theUserRepository.getUserByEmail(theNewUser.getEmail());
    
    // 2. Verificar:
    //    - ¿El usuario existe? (theActualUser != null)
    //    - ¿Está activo? (active != false)
    //    - ¿Tiene contraseña? (no es usuario solo-OAuth)
    //    - ¿La contraseña coincide? (comparar SHA-256)
    if (theActualUser != null &&
            !Boolean.FALSE.equals(theActualUser.getActive()) &&
            StringUtils.hasText(theActualUser.getPassword()) &&
            theActualUser.getPassword().equals(
                    theEncryptionService.convertSHA256(theNewUser.getPassword()))) {

        // 3. Si 2FA está habilitado → crear sesión pendiente
        if (twoFactorEnabled) {
            Session pendingSession = createPendingTwoFactorSession(theActualUser, "password");
            Map<String, Object> challenge = new HashMap<>();
            challenge.put("requires2fa", true);
            challenge.put("challengeToken", pendingSession.getToken());     // "2fa_uuid..."
            challenge.put("maskedEmail", maskEmail(theActualUser.getEmail())); // "ju***@***.com"
            challenge.put("expiresAt", pendingSession.getExpiration().getTime());
            challenge.put("remainingAttempts", pendingSession.getTwoFactorAttemptsLeft());
            return challenge;  // El frontend recibe esto y muestra pantalla de código 2FA
        }

        // 3b. Si 2FA está deshabilitado → generar JWT directamente
        String token = theJwtService.generateToken(theActualUser);
        createSession(theActualUser, token, "password");
        return Map.of("token", token);
    }
    return null;  // Credenciales incorrectas → el controller devuelve 401
}
```

#### Método `verifyTwoFactorCode()` — Verificar código 2FA

```java
public Map<String, Object> verifyTwoFactorCode(String challengeToken, String code) {
    // 1. Buscar la sesión pendiente de 2FA
    Session session = this.theSessionRepository.findPendingTwoFactorByToken(challengeToken);
    if (session == null || session.getUser() == null || session.getRevokedAt() != null) {
        return Map.of("status", "INVALID");  // No existe o ya fue revocada
    }

    // 2. ¿El código expiró?
    if (session.getExpiration().before(new Date())) {
        session.setRevokedAt(new Date());    // Marcarla como revocada
        this.theSessionRepository.save(session);
        return Map.of("status", "EXPIRED");
    }

    // 3. ¿El código es incorrecto?
    if (!code.equals(session.getCode2FA())) {
        int attemptsLeft = session.getTwoFactorAttemptsLeft() - 1;
        session.setTwoFactorAttemptsLeft(attemptsLeft);

        if (attemptsLeft == 0) {
            session.setRevokedAt(new Date());  // Sin intentos → cerrar sesión
            return Map.of("status", "LOCKED", "attemptsLeft", 0);
        }
        return Map.of("status", "INVALID_CODE", "attemptsLeft", attemptsLeft);
    }

    // 4. ¡Código correcto! → Generar JWT real
    String token = this.theJwtService.generateToken(session.getUser());
    session.setToken(token);                          // Reemplazar "2fa_uuid" por JWT real
    session.setJti(this.theJwtService.getTokenId(token));
    session.setExpiration(new Date(System.currentTimeMillis() + jwtExpiration));
    session.setCode2FA(null);                          // Limpiar código 2FA
    session.setTwoFactorAttemptsLeft(null);            // Limpiar intentos
    session.setTwoFactorVerifiedAt(new Date());        // Registrar verificación
    this.theSessionRepository.save(session);

    return Map.of("status", "OK", "token", token);     // Frontend guarda el JWT
}
```

#### Método `oauthLogin()` — Login con Google/Microsoft/GitHub

```java
public HashMap<String, Object> oauthLogin(String firebaseIdToken, String avatarHint) {
    // 1. Verificar el token de Firebase (le pregunta a Google: "¿este token es válido?")
    FirebaseToken firebaseToken = this.firebaseAuthService.verifyIdToken(firebaseIdToken);
    
    // 2. Obtener el proveedor (google.com, microsoft.com, github.com)
    String provider = this.firebaseAuthService.getProvider(firebaseToken);
    
    // 3. ¿Es un proveedor permitido?
    if (!isAllowedOAuthProvider(provider)) {
        throw new IllegalArgumentException("Proveedor OAuth no permitido");
    }

    // 4. Buscar o crear el usuario en nuestra BD
    User user = upsertOAuthUser(firebaseToken, provider, avatarHint);

    // 5. Generar JWT y crear sesión
    String token = this.theJwtService.generateToken(user);
    createSession(user, token, provider);

    // 6. Devolver token + datos del usuario + roles
    HashMap<String, Object> response = new HashMap<>();
    response.put("token", token);
    response.put("user", sanitizeUser(user));      // Usuario SIN la contraseña
    response.put("roles", getRolesByUserId(user.getId()));
    return response;
}
```

#### Método `upsertOAuthUser()` — Buscar o crear usuario OAuth

```java
private User upsertOAuthUser(FirebaseToken firebaseToken, String provider, String avatarHint) {
    String firebaseUid = firebaseToken.getUid();
    String email = firebaseToken.getEmail();

    // Estrategia de búsqueda en cascada:
    User user = this.theUserRepository.getUserByFirebaseUid(firebaseUid);    // 1. Por UID actual
    if (user == null) {
        user = this.theUserRepository.getUserByPreviousFirebaseUid(firebaseUid); // 2. Por UID anterior
    }
    if (user == null) {
        user = this.theUserRepository.getUserByEmail(email);                 // 3. Por email
    }

    if (user == null) {
        // No existe → crear uno nuevo
        user = new User();
        user.setEmail(email);
        user.setName(firebaseToken.getName());
        user.setActive(true);
    }

    // Actualizar datos de OAuth
    user.setFirebaseUid(firebaseUid);
    user.setAuthProvider(provider);
    user.setAvatar(extractProfilePhotoUrl(firebaseToken, avatarHint));
    user.setLastLoginAt(new Date());

    User saved = this.theUserRepository.save(user);
    assignDefaultRoleIfNeeded(saved);  // Asignar rol CIUDADANO si no tiene ningún rol
    return saved;
}
```

#### Método `getUserPayloadFromToken()` — Endpoint `/security/me`

```java
public Map<String, Object> getUserPayloadFromToken(String token) {
    // 1. Extraer datos del usuario desde el JWT
    User user = this.theJwtService.getUserFromToken(token);
    if (user == null) return null;

    // 2. Verificar que el usuario todavía existe en la BD
    User currentUser = this.theUserRepository.findById(user.getId()).orElse(null);
    if (currentUser == null) return null;

    // 3. Verificar que la sesión sigue activa (no fue revocada)
    Session activeSession = this.theSessionRepository.findActiveByToken(token);
    if (activeSession == null || Boolean.FALSE.equals(currentUser.getActive())) return null;

    // 4. Construir el payload con datos frescos de la BD
    HashMap<String, Object> payload = new HashMap<>();
    payload.put("user", sanitizeUser(currentUser));                // Datos del usuario (sin password)
    payload.put("roles", getRolesByUserId(currentUser.getId()));   // ["CIUDADANO", "CONDUCTOR"]
    payload.put("permissions", getPermissionsByUserId(currentUser.getId())); // [{url,method},...]
    return payload;
}
```

> [!IMPORTANT]
> Este es el método que el frontend llama al **cargar cada página**. Lee los datos **frescos de la BD**, no del JWT. Así, si te cambian los roles o permisos, se refleja **inmediatamente** sin necesidad de hacer login de nuevo.

#### Método `sanitizeUser()` — Limpiar datos sensibles

```java
private Map<String, Object> sanitizeUser(User user) {
    HashMap<String, Object> userPayload = new HashMap<>();
    userPayload.put("id", user.getId());
    userPayload.put("name", user.getName());
    userPayload.put("email", user.getEmail());
    userPayload.put("avatar", user.getAvatar());
    // NOTA: NO incluye password, resetPasswordToken, ni otros campos sensibles
    return userPayload;
}
```

---

### 📄 [ValidatorsService.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/services/ValidatorsService.java) — El verificador de permisos

Este servicio es el que **realmente decide** si una petición está autorizada:

```java
public boolean validationRolePermission(HttpServletRequest request, String url, String method) {
    // PASO 1: Extraer y verificar el userId del token JWT
    String userId = getVerifiedUserId(request);
    if (userId == null) return false;  // Token inválido o usuario eliminado

    // PASO 2: Excepciones para operaciones sobre uno mismo
    if (isSelfUserUpdate(url, method, userId) ||       // PUT /api/users/{miId}
        isSelfUserRoleRead(url, method, userId) ||     // GET /user-role/user/{miId}
        isOwnRolePermissionRead(url, method, userId)) { // GET /role-permission/role/{miRolId}
        return true;  // El usuario siempre puede ver/editar su propio perfil
    }

    // PASO 3: Normalizar la URL
    // "/api/users/6623a9f2e1b4c3d5e6f7a8b9" → "/api/users/?"
    url = url.replaceAll("[0-9a-fA-F]{24}|\\d+", "?");
    
    // PASO 4: Buscar si ese permiso existe en la BD
    Permission thePermission = this.thePermissionRepository.getPermission(url, method);
    if (thePermission == null) return false;  // No existe ese permiso → no hay acceso

    // PASO 5: Verificar que algún rol del usuario tenga ese permiso
    List<UserRole> roles = this.theUserRoleRepository.getRolesByUser(userId);
    for (UserRole ur : roles) {
        Role theRole = ur.getRole();
        if (theRole != null) {
            RolePermission rp = this.theRolePermissionRepository
                    .getRolePermission(theRole.getId(), thePermission.getId());
            if (rp != null) return true;  // ¡Encontramos un rol con ese permiso!
        }
    }
    return false;  // Ningún rol del usuario tiene ese permiso
}
```

#### `getVerifiedUserId()` — Verificación triple

```java
private String getVerifiedUserId(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);  // Quitar "Bearer " del inicio
        
        // Verificación 1: ¿El JWT es válido (firma correcta y no expirado)?
        if (!jwtService.validateToken(token)) return null;
        
        // Verificación 2: ¿Existe una sesión activa con ese token?
        if (this.theSessionRepository.findActiveByToken(token) == null) return null;
        
        // Verificación 3: ¿El usuario todavía existe en la BD y está activo?
        User userFromToken = jwtService.getUserFromToken(token);
        User user = this.theUserRepository.findById(userFromToken.getId()).orElse(null);
        if (user != null && !Boolean.FALSE.equals(user.getActive())) {
            return user.getId();
        }
    }
    return null;  // Cualquier fallo → no autorizado
}
```

> [!NOTE]
> Esta **verificación triple** garantiza que: (1) el token no fue manipulado, (2) la sesión no fue cerrada/revocada, y (3) el usuario no fue eliminado o desactivado. Aunque el JWT sea válido por 1 hora, si un admin elimina al usuario, pierde acceso inmediatamente.

---

### 📄 [RecaptchaVerificationService.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/services/RecaptchaVerificationService.java) — Anti-bots

```java
public boolean verifyToken(String token, String expectedAction, String remoteIp) {
    if (!recaptchaEnabled) return true;  // Si está deshabilitado, dejar pasar todo
    if (!StringUtils.hasText(token)) return false;  // Sin token → rechazar

    // Construir la petición a la API de reCAPTCHA Enterprise
    String url = String.format(
        "https://recaptchaenterprise.googleapis.com/v1/projects/%s/assessments?key=%s",
        enterpriseProjectId, enterpriseApiKey);

    // El "event" contiene: el token del frontend, la acción esperada, y el siteKey
    EnterpriseEvent event = new EnterpriseEvent();
    event.setToken(token);
    event.setExpectedAction(expectedAction);  // "login", "password_recovery_request", etc.
    event.setSiteKey(enterpriseSiteKey);

    // Enviar a Google y obtener respuesta
    EnterpriseAssessmentResponse response = restTemplate.postForObject(url, request, ...);

    // Verificar:
    // 1. ¿El token es válido?
    if (!response.getTokenProperties().getValid()) return false;
    
    // 2. ¿La acción coincide? (evita que alguien reutilice un token de login en recovery)
    if (!expectedAction.equals(response.getTokenProperties().getAction())) return false;
    
    // 3. ¿El score es suficiente? (>= 0.5)
    //    Score 1.0 = humano legítimo, Score 0.0 = bot seguro
    Double score = response.getRiskAnalysis().getScore();
    return score != null && score >= minScore;  // minScore = 0.5
}
```

---

### 📄 [FirebaseAuthService.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/services/FirebaseAuthService.java) — OAuth con Firebase

```java
@Service
public class FirebaseAuthService {
    private FirebaseAuth firebaseAuth;

    // Verifica un token de Firebase (se inicializa lazy)
    public synchronized FirebaseToken verifyIdToken(String idToken) {
        ensureInitialized();  // Se asegura de que Firebase está configurado
        return firebaseAuth.verifyIdToken(idToken, true);  // true = verificar revocación
    }

    // Extrae el proveedor de login (google.com, microsoft.com, github.com)
    public String getProvider(FirebaseToken token) {
        // Los claims del token contienen: { "firebase": { "sign_in_provider": "google.com" } }
        Object firebaseClaim = token.getClaims().get("firebase");
        if (firebaseClaim instanceof Map<?, ?> claimsMap) {
            return claimsMap.get("sign_in_provider").toString();
        }
        return "unknown";
    }
```

La inicialización es **lazy** (solo cuando se necesita por primera vez):

```java
    private void ensureInitialized() {
        if (firebaseAuth != null) return;  // Ya está inicializado

        // Buscar credenciales en múltiples ubicaciones:
        // 1. Variable de entorno GOOGLE_APPLICATION_CREDENTIALS
        // 2. Variable de entorno FIREBASE_CREDENTIALS_PATH
        // 3. Propiedad firebase.credentials.path
        // 4. confidential/credentials.json
        // 5. ../ms-notificaciones/confidential/credentials.json
        String credentialsPath = findCredentialsPath();

        if (credentialsPath != null) {
            // Cargar desde archivo
            optionsBuilder.setCredentials(GoogleCredentials.fromStream(new FileInputStream(path)));
        } else {
            // Intentar Application Default Credentials (ADC)
            optionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
        }

        FirebaseApp app = FirebaseApp.initializeApp(options);
        firebaseAuth = FirebaseAuth.getInstance(app);
    }
```

---

### 📄 [EmailNotificationService.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/services/EmailNotificationService.java) — Notificaciones por email

```java
@Service
public class EmailNotificationService {

    @Value("${notification.service.url:http://localhost:5000}")
    private String notificationServiceUrl;  // URL del ms-notificaciones

    private final RestTemplate restTemplate = new RestTemplate();

    // Ejemplo: notificar cambio de rol
    @Async  // Se ejecuta en un hilo aparte (no bloquea la respuesta al usuario)
    public void sendRoleChangeNotification(String userEmail, String userName,
                                           String roleName, String action) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("to", userEmail);
            body.put("name", userName);
            body.put("roleName", roleName);
            body.put("action", action);  // "Asignación" o "Revocación"
            
            // Hace un POST HTTP al microservicio de notificaciones
            restTemplate.postForObject(
                notificationServiceUrl + "/send-role-change", body, Map.class);
        } catch (Exception e) {
            // Si falla, solo loguea el error (no rompe el flujo principal)
            System.err.println("Error al enviar notificación: " + e.getMessage());
        }
    }
    
    // Mismo patrón para: sendPermissionChangeNotification, sendAccountConfirmationNotification,
    // sendPasswordRecoveryNotification, sendTwoFactorCodeNotification
}
```

> [!TIP]
> Este servicio **no envía emails directamente**. Solo llama a `ms-notificaciones` por HTTP. Eso separa responsabilidades: ms-security decide **cuándo** enviar, ms-notificaciones decide **cómo** (templates, SMTP, etc).

---

### 📄 [UserService.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/services/UserService.java) — CRUD de usuarios

#### `create()` — Crear usuario

```java
public User create(User newUser) {
    // Validaciones
    if (newUser.getEmail() == null || newUser.getPassword() == null)
        throw new IllegalArgumentException("Correo y contraseña obligatorios");
    
    if (!isPasswordValid(newUser.getPassword()))  // 8 chars, 1 mayúsc, 1 número, 1 especial
        throw new IllegalArgumentException("Contraseña no cumple requisitos");

    if (this.theUserRepository.getUserByEmail(newUser.getEmail()) != null)
        return null;  // Email ya registrado

    // Cifrar contraseña y guardar
    newUser.setPassword(this.theEncryption.convertSHA256(newUser.getPassword()));
    User saved = this.theUserRepository.save(newUser);
    
    // Asignar rol CIUDADANO automáticamente
    Role ciudadano = this.theRoleRepository.findByName("CIUDADANO");
    if (ciudadano != null) {
        this.theUserRoleRepository.save(new UserRole(saved, ciudadano));
    }

    // Enviar email de confirmación
    emailNotificationService.sendAccountConfirmationNotification(saved.getEmail(), saved.getName());
    return saved;
}
```

#### `update()` — Actualizar usuario (con lógica de desvinculación OAuth)

```java
public User update(String id, User newUser) {
    User actualUser = this.theUserRepository.findById(id).orElse(null);
    if (actualUser == null) return null;

    // Actualizar nombre y email (con validación de duplicados)
    if (StringUtils.hasText(newUser.getName())) actualUser.setName(newUser.getName());
    if (StringUtils.hasText(newUser.getEmail())) {
        User emailOwner = this.theUserRepository.getUserByEmail(newUser.getEmail());
        if (emailOwner != null && !id.equals(emailOwner.getId()))
            throw new IllegalArgumentException("El correo ya está registrado");
        actualUser.setEmail(newUser.getEmail());
    }

    // ¿El usuario quiere desvincular su cuenta social?
    boolean unlinkSocial = Boolean.TRUE.equals(newUser.getUnlinkSocialAccount());
    
    if (unlinkSocial) {
        // Debe definir una contraseña nueva (porque antes entraba con OAuth)
        actualUser.setPassword(this.theEncryption.convertSHA256(newUser.getPassword()));
        actualUser.setPreviousFirebaseUid(actualUser.getFirebaseUid()); // Guardar UID viejo
        actualUser.setFirebaseUid(null);
        actualUser.setAuthProvider(null);
        
        // Eliminar la identidad en Firebase
        removeFirebaseIdentity(firebaseUidToDelete, emailToDelete);
    } else if (StringUtils.hasText(newUser.getPassword())) {
        // Solo cambio de contraseña normal
        actualUser.setPassword(this.theEncryption.convertSHA256(newUser.getPassword()));
    }

    this.theUserRepository.save(actualUser);
    return actualUser;
}
```

---

### 📄 [RolePermissionService.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/services/RolePermissionService.java) — Asignar permisos a roles

```java
public boolean addRolePermission(String roleId, String permissionId) {
    Role role = this.theRoleRepository.findById(roleId).orElse(null);
    Permission permission = this.thePermissionRepository.findById(permissionId).orElse(null);
    if (role != null && permission != null) {
        this.theRolePermissionRepository.save(new RolePermission(role, permission));
        
        // ¡Importante! Notificar a TODOS los usuarios que tienen este rol
        notifyUsersOfPermissionChange(roleId, role.getName());
        return true;
    }
    return false;
}

// Busca todos los usuarios con ese rol y les envía email
private void notifyUsersOfPermissionChange(String roleId, String roleName) {
    List<UserRole> userRoles = this.theUserRoleRepository.getUsersByRole(roleId);
    for (UserRole ur : userRoles) {
        if (ur.getUser() != null) {
            emailNotificationService.sendPermissionChangeNotification(
                    ur.getUser().getEmail(), ur.getUser().getName(), roleName);
        }
    }
}
```

> [!IMPORTANT]
> Los cambios en permisos se aplican **inmediatamente** porque el `ValidatorsService` consulta la BD en cada request. No hace falta que el usuario cierre sesión y vuelva a entrar.

---

## 5️⃣ Interceptor — [SecurityInterceptor.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/interceptors/SecurityInterceptor.java)

```java
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private ValidatorsService validatorService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        
        // Los navegadores envían OPTIONS antes de cada petición con headers custom
        // Esto se llama "preflight CORS" — siempre hay que dejarlo pasar
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        // Delegar la validación al ValidatorsService
        boolean allowed = this.validatorService.validationRolePermission(
                request,
                request.getRequestURI(),    // Ej: "/api/buses/abc123"
                request.getMethod()         // Ej: "GET"
        );
        
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"No autorizado\"}");
        }
        return allowed;  // true = dejar pasar al controller, false = bloquear
    }
}
```

---

## 6️⃣ Configuración — [WebConfig.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/configurations/WebConfig.java)

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                // ✅ PROTEGER estas rutas (necesitan token + permisos)
                .addPathPatterns("/api/**")           // Usuarios, buses, rutas, etc.
                .addPathPatterns("/roles/**")          // CRUD de roles
                .addPathPatterns("/permissions/**")    // CRUD de permisos
                .addPathPatterns("/role-permission/**")// Asignación de permisos a roles
                .addPathPatterns("/user-role/**")      // Asignación de roles a usuarios
                .addPathPatterns("/profiles/**")       // Perfiles
                .addPathPatterns("/sessions/**")       // Sesiones
                
                // ❌ EXCLUIR estas rutas (son públicas)
                .excludePathPatterns("/api/public/**")     // Endpoints públicos
                .excludePathPatterns("/api/users/register")// Registro sin token
                .excludePathPatterns("/security/**");      // Login, 2FA, OAuth, recovery
    }
}
```

---

## 7️⃣ DataInitializer — [DataInitializer.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/configurations/DataInitializer.java)

Se ejecuta **automáticamente al arrancar la aplicación** (porque implementa `CommandLineRunner`):

```java
@Component
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        initializeDefaultRoles();           // 1. Crear 5 roles si no existen
        initializeDefaultPermissions();     // 2. Crear ~40 permisos si no existen
        initializeAdminRolePermissions();   // 3. Asignar TODOS los permisos al ADMINISTRADOR_SISTEMA
    }
}
```

Cada método verifica si los datos ya existen antes de crearlos, evitando duplicados al reiniciar el servidor.

---

## 8️⃣ Controllers — Carpeta `controllers/`

### 📄 [SecurityController.java](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/controllers/SecurityController.java) — El más complejo

Es el controlador **público** (no pasa por el interceptor). Maneja toda la autenticación:

| Método | Ruta | Lo que hace internamente |
|--------|------|--------------------------|
| `login()` | `POST /security/login` | Valida reCAPTCHA → llama `securityService.login()` → devuelve JWT o challenge 2FA |
| `verifyTwoFactor()` | `POST /security/2fa/verify` | Llama `securityService.verifyTwoFactorCode()` → devuelve JWT si el código es correcto |
| `resendTwoFactor()` | `POST /security/2fa/resend` | Regenera el código y lo reenvía por email |
| `cancelTwoFactor()` | `POST /security/2fa/cancel` | Revoca la sesión pendiente |
| `oauthLogin()` | `POST /security/oauth/login` | Verifica token Firebase → crea/actualiza usuario → devuelve JWT |
| `requestPasswordRecovery()` | `POST /security/password-recovery/request` | Genera token UUID → envía email con link |
| `resetPassword()` | `POST /security/password-recovery/reset` | Verifica token → actualiza contraseña |
| `me()` | `GET /security/me` | Extrae token del header → devuelve usuario + roles + permisos |
| `logout()` | `POST /security/logout` | Revoca la sesión activa (pone `revokedAt`) |

### 📄 Controllers CRUD — Patrón repetido

Los demás controllers ([UserController](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/controllers/UserController.java), [RoleController](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/controllers/RoleController.java), [PermissionController](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/controllers/PermissionController.java), etc.) siguen todos el mismo patrón:

```java
@CrossOrigin              // Permite peticiones desde cualquier origen (CORS)
@RestController            // Devuelve JSON automáticamente
@RequestMapping("/ruta")   // Ruta base
public class XxxController {

    @Autowired
    private XxxService theService;

    @GetMapping("")                           // GET /ruta       → Listar todos
    @GetMapping("{id}")                       // GET /ruta/123   → Obtener uno
    @PostMapping                              // POST /ruta      → Crear
    @PutMapping("{id}")                       // PUT /ruta/123   → Actualizar
    @DeleteMapping("{id}")                    // DELETE /ruta/123 → Eliminar
}
```

**Excepción notable**: [UserController](file:///c:/Desarrollo%20backend/proyecto_buses/ms-security/src/main/java/com/sho/ms_security/controllers/UserController.java) tiene un endpoint especial `POST /api/users/register` que es **público** (excluido del interceptor), permitiendo el registro sin token.

---

## 🔗 ¿Cómo se conecta todo? — Flujo completo de una petición

```
1. Frontend envía: GET /api/buses (con header "Authorization: Bearer eyJ...")

2. WebConfig: "/api/**" está en addPathPatterns → activar SecurityInterceptor

3. SecurityInterceptor.preHandle():
   → No es OPTIONS → continuar
   → Llama validatorsService.validationRolePermission(request, "/api/buses", "GET")

4. ValidatorsService.validationRolePermission():
   → getVerifiedUserId(): extrae token → valida JWT → busca sesión activa → busca usuario en BD
   → No es self-update → continuar
   → Normaliza URL: "/api/buses" → "/api/buses" (sin cambios, no tiene IDs)
   → Busca Permission: {url: "/api/buses", method: "GET"} → ¡existe!
   → Busca roles del usuario → [CIUDADANO]
   → ¿CIUDADANO tiene permiso GET /api/buses? → Busca RolePermission → ¿existe?
     → Si SÍ: return true → el interceptor deja pasar → llega al BusController
     → Si NO: return false → interceptor devuelve 401 Unauthorized

5. Si pasó: el controller procesa la petición y devuelve los datos
```
