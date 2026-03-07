# Usar MongoDB Atlas (nube) para guardar desde cualquier dispositivo

Con **MongoDB Atlas** la base de datos está en la nube. Cualquier dispositivo (móvil, otra PC, servidor) que use tu aplicación verá los mismos usuarios.

## 1. Crear cuenta y cluster en Atlas

1. Entra en **https://cloud.mongodb.com** e inicia sesión (o crea cuenta gratis).
2. Crea un **cluster gratuito** (Shared / M0).
3. Elige una región cercana (ej. `AWS / São Paulo` o `N. Virginia`).
4. Espera a que el cluster esté listo.

## 2. Usuario de base de datos

1. En el menú izquierdo: **Database Access** → **Add New Database User**.
2. Usuario y contraseña (guárdalos; la contraseña no puede tener caracteres raros como `@` o `#` o ponlos codificados en la URI).
3. Permisos: **Atlas admin** o **Read and write to any database**.

## 3. Acceso desde cualquier IP

1. En el menú: **Network Access** → **Add IP Address**.
2. Para desarrollo: **Allow Access from Anywhere** (`0.0.0.0/0`).
3. En producción es mejor añadir solo las IPs de tu servidor.

## 4. Obtener la URI de conexión

1. **Database** → en tu cluster, clic en **Connect**.
2. Elige **Drivers** (o **Connect your application**).
3. Copia la URI. Se verá así:

```
mongodb+srv://TU_USUARIO:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
```

4. Sustituye `<password>` por tu contraseña de usuario de BD (si tiene `@` o `#`, codifícala en URL, ej. `@` → `%40`).
5. Opcional: añade el nombre de la base de datos en la URI:

```
mongodb+srv://TU_USUARIO:TU_PASSWORD@cluster0.xxxxx.mongodb.net/bustrack?retryWrites=true&w=majority
```

## 5. Configurar el backend

**Opción A – Variable de entorno (recomendado, no subas la contraseña a Git)**

En Windows (PowerShell):

```powershell
$env:MONGODB_URI="mongodb+srv://tu_usuario:tu_password@cluster0.xxxxx.mongodb.net/bustrack?retryWrites=true&w=majority"
```

Luego ejecuta el backend como siempre (por ejemplo `mvn spring-boot:run` o desde el IDE).

En Linux/Mac:

```bash
export MONGODB_URI="mongodb+srv://tu_usuario:tu_password@cluster0.xxxxx.mongodb.net/bustrack?retryWrites=true&w=majority"
```

**Opción B – Archivo local (no subir a Git)**

1. Crea `backend/src/main/resources/application-local.properties`.
2. Añade solo:

```properties
spring.data.mongodb.uri=mongodb+srv://tu_usuario:tu_password@cluster0.xxxxx.mongodb.net/bustrack?retryWrites=true&w=majority
```

3. Ejecuta el backend con perfil `local`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

O en el IDE: en “Active profiles” pon `local`.

## 6. Comprobar

Al arrancar el backend, si la URI es correcta, Spring se conecta a Atlas. Los registros y logins se guardan en la base `bustrack` en la nube y serán los mismos desde cualquier dispositivo que use tu API.

## Seguridad

- No subas la URI (ni `application-local.properties` con la URI) a GitHub. Añade `application-local.properties` al `.gitignore` si lo usas.
- En producción usa variables de entorno en el servidor para `MONGODB_URI`.
