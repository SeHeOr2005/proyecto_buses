# Backend — Sistema de Transporte SHO

Monorepo con los microservicios del backend del sistema de gestión de transporte.

## Microservicios

| Servicio | Tecnología | Puerto | Descripción |
|---|---|---|---|
| `ms-security` | Spring Boot 4 / Java 17 | 8080 | Autenticación, usuarios, roles y permisos |
| `ms-notificaciones` | Flask 3 / Python | 5000 | Envío de correos via Gmail API |

---

## ms-security

Microservicio de seguridad que implementa autenticación JWT y control de acceso basado en roles (RBAC).

### Qué hace

- Gestión completa de usuarios (crear, listar, buscar, actualizar, eliminar)
- Gestión de roles: 5 roles predeterminados + creación de roles personalizados
- Permisos granulares por módulo (lectura, escritura, edición, eliminación)
- Interceptor de seguridad que valida token y permisos en cada petición
- Notifica por email al usuario cuando sus roles o permisos cambian

### Roles predeterminados

| Rol | Descripción |
|---|---|
| `ADMINISTRADOR_SISTEMA` | Acceso total al sistema |
| `ADMINISTRADOR_EMPRESA` | Administrador de empresa de transporte |
| `SUPERVISOR` | Supervisor de operaciones |
| `CONDUCTOR` | Conductor de unidad |
| `CIUDADANO` | Usuario ciudadano |

### Dependencias

- Java 17
- Maven
- MongoDB Atlas (base de datos en la nube)
- Conexión a `ms-notificaciones` para envío de emails

### Configuración

Crea un archivo `.env` en `ms-security/` basado en `.env.example`:

```env
MONGODB_URI=mongodb+srv://<usuario>:<password>@<cluster>.mongodb.net/
MONGODB_DATABASE=db_security
JWT_SECRET=clave_secreta_minimo_32_caracteres
JWT_EXPIRATION=3600000
NOTIFICATION_SERVICE_URL=http://localhost:5000
FIREBASE_PROJECT_ID=flashbuslogin
FIREBASE_CREDENTIALS_PATH=C:\\ruta\\a\\firebase-service-account.json
```

> Las variables de entorno se cargan automáticamente al ejecutar con el script de Maven.
>
> Para OAuth con Firebase (Google/correo-contraseña), descarga una credencial de **Service Account** desde Firebase Console y define `FIREBASE_CREDENTIALS_PATH` apuntando al JSON.

### Ejecutar

```bash
cd ms-security

# Windows
set MONGODB_URI=tu_uri
set MONGODB_DATABASE=db_security
set JWT_SECRET=tu_clave_secreta
./mvnw spring-boot:run

# Mac/Linux
export MONGODB_URI=tu_uri
export MONGODB_DATABASE=db_security
export JWT_SECRET=tu_clave_secreta
./mvnw spring-boot:run
```

### Endpoints principales

| Método | Endpoint | Auth | Descripción |
|---|---|---|---|
| POST | `/security/login` | No | Obtener JWT |
| POST | `/security/oauth/login` | No | Login con `firebaseIdToken` (Firebase Auth) |
| POST | `/api/users/register` | No | Registrar primer usuario |
| GET | `/api/users` | Sí | Listar usuarios |
| GET | `/api/users/search?query=` | Sí | Buscar por nombre o email |
| GET | `/roles` | No | Listar roles |
| POST | `/roles` | No | Crear rol |
| DELETE | `/roles/{id}` | No | Eliminar rol (valida que no tenga usuarios) |
| GET | `/permissions` | No | Listar permisos |
| GET | `/role-permission/role/{id}` | No | Ver permisos de un rol |
| POST | `/role-permission/role/{r}/permission/{p}` | No | Asignar permiso a rol |
| DELETE | `/role-permission/{id}` | No | Quitar permiso de rol |
| GET | `/user-role/user/{id}` | No | Ver roles de un usuario |
| POST | `/user-role/user/{u}/role/{r}` | No | Asignar rol a usuario |
| DELETE | `/user-role/{id}` | No | Revocar rol de usuario |

---

## ms-notificaciones

Microservicio de notificaciones que envía correos HTML usando Gmail API con OAuth2.

### Qué hace

- Envío genérico de correos (texto plano o HTML)
- Notificación automática cuando se asigna o revoca un rol a un usuario
- Notificación automática cuando los permisos de un rol cambian

### Dependencias

- Python 3.10+
- Cuenta de Google con Gmail API habilitada
- Archivo `credentials.json` obtenido desde Google Cloud Console

### Instalación

```bash
cd ms-notificaciones

# Crear entorno virtual
python -m venv venv

# Activar entorno virtual
venv\Scripts\activate       # Windows
source venv/bin/activate    # Mac/Linux

# Instalar dependencias
pip install -r requirements.txt
```

### Configuración

1. Crea la carpeta `confidential/` dentro de `ms-notificaciones/`
2. Coloca tu `credentials.json` de Google Cloud Console dentro de `confidential/`
3. La primera vez que ejecutes, abrirá el navegador para autorizar con tu cuenta de Google
4. Se generará `confidential/token.pickle` automáticamente

### Ejecutar

```bash
cd ms-notificaciones
venv\Scripts\activate   # Windows
python app.py
```

La primera ejecución solicitará autorización en el navegador. Las siguientes arrancan directamente.

### Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/health` | Verificar que el servicio está activo |
| POST | `/send-email` | Envío genérico de correo |
| POST | `/send-role-change` | Notificación de cambio de rol |
| POST | `/send-permission-change` | Notificación de cambio de permisos |

---

## Ejecutar todo junto

Abre **dos terminales** y ejecuta cada microservicio:

**Terminal 1 — ms-notificaciones**
```bash
cd ms-notificaciones
venv\Scripts\activate
python app.py
# Disponible en http://localhost:5000
```

**Terminal 2 — ms-security**
```bash
cd ms-security

# Configura las variables de entorno primero (ver sección de configuración)
./mvnw spring-boot:run
# Disponible en http://localhost:8080
```

## Flujo de uso inicial

```
1. Registrar usuario admin    →  POST /api/users/register
2. Login                      →  POST /security/login  (guarda el token)
3. Obtener ID del rol admin    →  GET  /roles
4. Asignar rol al usuario      →  POST /user-role/user/{id}/role/{id}
5. Usar endpoints protegidos   →  GET  /api/users  (con Bearer token)
```
