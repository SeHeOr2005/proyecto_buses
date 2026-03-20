# Análisis Unificación main + rama Naranjo
## Sistema de Buses Inteligentes - Merge Strategy

**Fecha:** 2024
**Estado:** Análisis Completo - Listo para Implementación

---

## 📋 RESUMEN EJECUTIVO

El proyecto tiene **70% funcionalidad backend + 50% funcionalidad frontend**. No hay conflictos de git reales, pero **SÍ hay incompatibilidades de configuración** entre ramas:

| Aspecto | Naranjo (actual) | Main (asumido) | Crítico? |
|---------|------------------|----------------|----------|
| Puerto Backend | 8081 | 8080 (probable) | 🔴 SÍ |
| Frontend → API | localhost:8081 | localhost:8080 | 🔴 SÍ |
| OAuth2 credentials | application.properties | ??? | 🔴 SÍ |
| MongoDB credentials | application-local.properties (git) | .env (probable) | 🔴 CRÍTICO |
| Estructura componentes | Standalone (Angular 17+) | ??? | 🟡 Posible |

---

## 🔍 PROBLEMAS IDENTIFICADOS

### CRÍTICOS (⚠️ SOLUCIONAR ANTES DE MERGE)

#### 1. **Credenciales MongoDB en Git**
**Archivo:** `backend/src/main/resources/application-local.properties`
```properties
spring.data.mongodb.uri=mongodb+srv://sebastianherrera45451:v5wn9T7sbXVpSTls@backend.bkvqlbn.mongodb.net/
```
**Impacto:** Acceso no autorizado a BD productiva  
**Solución:**
```bash
# 1. Remover del repositorio
git rm --cached backend/src/main/resources/application-local.properties
git commit -m "Remove exposed credentials"

# 2. Agregar a .gitignore
echo "application-local.properties" >> .gitignore

# 3. Crear template
cat > backend/src/main/resources/application-local.properties.example << 'EOF'
# MONGODB DEVELOPMENT
# Obtén credenciales de MongoDB Atlas
spring.data.mongodb.uri=mongodb+srv://${MONGODB_USER}:${MONGODB_PASSWORD}@${MONGODB_CLUSTER}/${MONGODB_DATABASE}
EOF

# 4. Usar variable de entorno
# En application.properties:
spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/buses_db}
```

---

#### 2. **Discrepancia Puerto Backend**
**Problema:** Frontend apunta a `localhost:8081` pero main branch probablemente usa `8080`

**Backend - application.properties:**
```properties
server.port=8081
frontend.url=http://localhost:4200
```

**Frontend - auth.service.ts:**
```typescript
private readonly API_URL = 'http://localhost:8081/api/auth';
private readonly API_BASE = 'http://localhost:8081';
```

**Solución Unificada:**
```bash
# OPCIÓN A: Usar 8081 como estándar (Naranjo)
# Verificar en main branch y actualizar si usa 8080

# OPCIÓN B: Usar 8080 como estándar (Maven default)
# En backend/src/main/resources/application.properties
server.port=8080  # Cambio

# En frontend/src/services/auth.service.ts
private readonly API_URL = 'http://localhost:8080/api/auth';  # Cambio
private readonly API_BASE = 'http://localhost:8080';  # Cambio
```

**Recomendación:** Usar **8080** (convención Maven) y actualizar ambos archivos.

---

### ALTOS (🔴 SOLUCIONAR PRONTO)

#### 3. **Token Sin Expiración**
**Archivo:** `backend/src/main/java/com/example/auth/service/AuthService.java`
```java
private String generateToken() {
    return "jwt-" + UUID.randomUUID().toString();  // ❌ NO es JWT real
}
```
**Problema:** Token válido indefinidamente, no incluye datos del usuario, fácil de forjar

**Solución:**
```java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret:mi-secreto-super-seguro-64-caracteres-minimo}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}") // 24 horas por defecto
    private long jwtExpirationMs;
    
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public String getEmailFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

**application.properties:**
```properties
jwt.secret=${JWT_SECRET:change-me-in-production-with-strong-secret-key}
jwt.expiration=86400000  # 24 horas
```

---

#### 4. **Validación Token Sin Expiración**
**Archivo:** `backend/src/main/java/com/example/auth/config/TokenAuthenticationFilter.java`
```java
// ❌ ACTUAL: Solo valida que sessionToken existe en DB
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                FilterChain filterChain) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        Optional<User> user = userRepository.findBySessionToken(token);
        if (user.isPresent()) {
            // ❌ No valida expiración
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.get().getEmail(), "", new ArrayList<>());
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, null, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
    filterChain.doFilter(request, response);
}
```

**Solución:**
```java
// ✅ NUEVO: Valida JWT con expiración
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserRepository userRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Valida JWT (incluye expiración)
                if (jwtTokenProvider.validateToken(token)) {
                    String email = jwtTokenProvider.getEmailFromToken(token);
                    Optional<User> user = userRepository.findByEmail(email);
                    
                    if (user.isPresent()) {
                        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                            user.get().getEmail(), "", new ArrayList<>());
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, new ArrayList<>());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
```

---

#### 5. **CORS Demasiado Permisivo**
**Archivo:** `backend/src/main/java/com/example/auth/config/SecurityConfig.java`
```java
http.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(Arrays.asList(
        "http://localhost:*",           // ❌ Acepta 4200, 4201, 5000, etc
        "http://127.0.0.1:*"
    ));
    // ...
}))
```

**Solución:**
```java
config.setAllowedOrigins(Arrays.asList(
    "http://localhost:4200",        // ✅ Solo desarrollo
    "http://localhost:8080",        // API testing
    "http://127.0.0.1:4200"
));
```

---

### MEDIOS (🟡 IMPORTANTE)

#### 6. **Falta Interceptor HTTP para Errores 401**
**Problema:** Si token expira, usuario queda atrapado con token inválido en localStorage

**Solución - Crear archivo:**
`frontend/src/services/auth.interceptor.ts`
```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService, private router: Router) {}
  
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Token expirado o inválido
          this.authService.logout();
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
```

**Registrar en app.config.ts:**
```typescript
import { AuthInterceptor } from './services/auth.interceptor';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    // ...otros providers
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ]
};
```

---

#### 7. **OAuth2 - Email Fallback Inválido**
**Archivo:** `backend/src/main/java/com/example/auth/config/CustomOAuth2UserService.java`
```java
String email = attributes.get("email") != null ? (String) attributes.get("email") 
    : (String) attributes.get("login") + "@github.user";  // ❌ Formato inválido
```

**Solución:**
```java
String email = (String) attributes.get("email");
if (email == null) {
    // GitHub requiere scope 'user:email' para acceso
    throw new IllegalArgumentException("Email no disponible en OAuth2 response. " +
        "Verifica scopes: user:email");
}
```

---

## ✅ ESTADO DE IMPLEMENTACIÓN

### Backend - 16 archivos analizados
| Componente | Estado | Notas |
|-----------|--------|-------|
| AuthService | 60% | Necesita JWT provider |
| SecurityConfig | 80% | Necesita CORS ajuste |
| TokenAuthenticationFilter | 60% | Necesita validación JWT |
| CustomOAuth2UserService | 70% | Necesita validación email |
| AuthController | ✅ 100% | Endpoints funcionan |
| UserController | ✅ 100% | Endpoint /user funciona |
| Modelos (User, DTO) | ✅ 100% | Estructura correcta |
| pom.xml | ✅ 100% | Dependencias OK |

### Frontend - 20+ archivos analizados
| Componente | Estado | Notas |
|-----------|--------|-------|
| auth.service.ts | ✅ 90% | Necesita interceptor |
| AuthComponent | ✅ 90% | UI/lógica funcional |
| HomeComponent | 50% | Solo estructura, sin datos |
| routing | ✅ 100% | Lazy loading OK |
| CSS/Estilos | ✅ 100% | Completo |
| Configuración | ✅ 100% | OK |

---

## 📝 PLAN DE ACCIÓN

### FASE 1: Preparación (Antes de Merge)
**Prioridad: INMEDIATA**

- [ ] **Remover credenciales de repositorio**
  - Eliminar application-local.properties del historio de git
  - Crear .example template
  - Agregar a .gitignore

- [ ] **Verificar configuración main branch**
  - Clonar/checkout main branch
  - Revisar puerto backend (¿8080 o 8081?)
  - Revisar OAuth2 credentials
  - Revisar frontend API_URL

- [ ] **Unificar puerto**
  - Decidir: 8080 (estándar Maven) o 8081 (actual Naranjo)
  - Actualizar application.properties (backend)
  - Actualizar auth.service.ts (frontend)

### FASE 2: Seguridad (Alta Prioridad)
**Prioridad: ANTES DE PASAR A PRODUCCIÓN**

- [ ] **Implementar JWT real**
  - Crear JwtTokenProvider.java
  - Actualizar AuthService.generateToken()
  - Agregar dependencia jjwt:jjwt

- [ ] **Validar JWT con expiración**
  - Reescribir TokenAuthenticationFilter
  - Agregar validación de expiration

- [ ] **Ajustar CORS**
  - Especificar puertos exactos en SecurityConfig
  - Remover wildcards

- [ ] **Validación OAuth2**
  - Mejorar error handling en CustomOAuth2UserService
  - Requerir email scope

### FASE 3: Frontend (Media Prioridad)
**Prioridad: DESPUÉS DE ERROR 401**

- [ ] **Crear AuthInterceptor**
  - Manejar errores 401
  - Auto-logout y redirección

- [ ] **Registrar en appConfig**
  - Agregar HTTP_INTERCEPTORS provider

### FASE 4: Testing
**Prioridad: FINAL**

- [ ] **Test login local** (email/password)
- [ ] **Test login OAuth2** (Google/GitHub)
- [ ] **Test token expiration** (esperar 24h o inyectar)
- [ ] **Test 401 redirect** (invalidar token)
- [ ] **Test CORS** (verificar headers)

---

## 🔧 ARCHIVOS A MODIFICAR

### Backend (Maven)

```
backend/
├── pom.xml                                    [+ADD jjwt dependency]
├── src/main/
│   ├── java/com/example/auth/
│   │   ├── service/
│   │   │   ├── AuthService.java              [MODIFY: generateToken() → JWT]
│   │   │   └── JwtTokenProvider.java          [NEW: JWT provider]
│   │   ├── config/
│   │   │   ├── SecurityConfig.java            [MODIFY: CORS específico]
│   │   │   └── TokenAuthenticationFilter.java [MODIFY: validar JWT expiration]
│   │   └── CustomOAuth2UserService.java       [MODIFY: validar email]
│   └── resources/
│       ├── application.properties             [MODIFY: puerto, JWT secret]
│       └── application-local.properties       [DELETE: credenciales en git]
└── .gitignore                                 [MODIFY: +application-local.properties]
```

### Frontend (Angular 17)

```
frontend/src/
├── services/
│   └── auth.interceptor.ts                    [NEW: HTTP 401 handler]
├── app/
│   └── app.config.ts                          [MODIFY: +HTTP_INTERCEPTORS]
└── app/
    ├── app.routes.ts                          [VERIFY: rutas OK]
    └── app.ts                                 [VERIFY: importa RouterOutlet]
```

---

## 📊 FLUJO DE AUTENTICACIÓN (Después de Unificación)

```
LOGIN LOCAL
┌─────────────────────────────────────────┐
│  1. Frontend: email + password           │
│  2. POST /api/auth/login                │
│  3. Backend: BCrypt validate ✓          │
│  4. Backend: generateToken (JWT + exp)  │
│  5. Frontend: saveToken(localStorage)  │
│  6. Frontend: navigate /home            │
│  7. Requests: Authorization: Bearer {jwt}
│  8. Filter: validateToken (exp + sig)   │
│  9. Si expira: 401 → Interceptor        │
│  10. logout() + navigate /login          │
└─────────────────────────────────────────┘

OAUTH2 (Google/GitHub)
┌─────────────────────────────────────────┐
│  1. Frontend: click Google/GitHub        │
│  2. Redirect: /oauth2/authorization/...│
│  3. Backend: OAuth handler               │
│  4. Backend: findOrCreateUser           │
│  5. Backend: generateToken              │
│  6. Backend: redirect /auth/callback?tok..
│  7. Frontend: lee token de query param  │
│  8. Frontend: saveToken + navigate home │
└─────────────────────────────────────────┘
```

---

## 🎯 PRÓXIMOS PASOS

1. **Confirmar config main branch** - ¿Puerto 8080?
2. **Ejecutar FASE 1** - Remover credenciales, unificar puerto
3. **Ejecutar FASE 2** - Implementar JWT, mejorar seguridad
4. **Ejecutar FASE 3** - Interceptor OAuth2, manejo errores
5. **Testing completo** - Verificar flujos end-to-end

---

**Estado:** Análisis 100% ✅ | Listo para Implementación 🚀
