# Implementación del Módulo de Negocio — ms-negocio (Node.js)

## Contexto

Se necesita crear un nuevo microservicio `ms-negocio` en **Node.js** dentro del monorepo `proyecto_buses`, que implemente el diagrama de clases del sistema de transporte urbano. Este microservicio coexistirá con `ms-security` (Spring Boot) y `ms-notificaciones` (Flask).

**Stack tecnológico:**
- **Runtime:** Node.js
- **Framework:** Express.js
- **ODM:** Mongoose (MongoDB)
- **Validación:** express-validator
- **Base de datos:** MongoDB Atlas (mismo cluster `ClusterNarver`, BD separada: `db_negocio`)

---

## Diagrama de clases identificado

Del diagrama proporcionado, se identifican **21 entidades** organizadas en los siguientes módulos:

### 🏢 Módulo Empresarial
| Entidad | Atributos clave | Relaciones |
|---------|----------------|------------|
| **Empresa** | id, nombre, nit, direccion, telefono, email, estado | 1:N → Bus |

### 🚌 Módulo de Flota
| Entidad | Atributos clave | Relaciones |
|---------|----------------|------------|
| **Bus** | id, placa, modelo, capacidad, estado, empresa_id | N:1 → Empresa, 1:1 → GPS, N:1 → Conductor |
| **GPS** | id, latitud, longitud, ultima_actualizacion, bus_id | 1:1 → Bus |

### 👤 Módulo de Personas
| Entidad | Atributos clave | Relaciones |
|---------|----------------|------------|
| **Persona** | id, nombre, apellido, documento, telefono, email, user_id (ref a ms-security) | Base (herencia) |
| **Conductor** | id, licencia, tipo_licencia, fecha_vencimiento_licencia, estado | Hereda → Persona, 1:N → Turno |
| **Ciudadano** | id, fecha_nacimiento | Hereda → Persona, 1:1 → Dirección, N:N → MetodoPago |
| **Dirección** | id, calle, ciudad, departamento, codigo_postal, ciudadano_id | 1:1 → Ciudadano |

### 🛣️ Módulo de Rutas
| Entidad | Atributos clave | Relaciones |
|---------|----------------|------------|
| **Ruta** | id, nombre, descripcion, distancia_km, duracion_estimada, estado | 1:N → Nodo, 1:N → Paradero |
| **Nodo** | id, nombre, latitud, longitud, orden, ruta_id | N:1 → Ruta |
| **Paradero** | id, nombre, latitud, longitud, direccion, ruta_id | N:1 → Ruta |

### 📅 Módulo de Programación
| Entidad | Atributos clave | Relaciones |
|---------|----------------|------------|
| **Programación** | id, fecha, hora_inicio, hora_fin, ruta_id, bus_id, estado | N:1 → Ruta, N:1 → Bus |
| **Turno** | id, fecha, hora_inicio, hora_fin, conductor_id, tipo | N:1 → Conductor |

### 🎫 Módulo de Boletos
| Entidad | Atributos clave | Relaciones |
|---------|----------------|------------|
| **Boleto** | id, fecha_compra, precio, estado, ciudadano_id, programacion_id, ruta_id | N:1 → Ciudadano, N:1 → Programación, N:1 → Ruta |
| **Historial** | id, accion, fecha, detalle, boleto_id | N:1 → Boleto |
| **MetodoPago** | id, nombre, tipo, activo | Independiente |
| **MetodoPagoCiudadano** | id, ciudadano_id, metodo_pago_id, detalle, predeterminado | N:1 → Ciudadano, N:1 → MetodoPago |

### ⚠️ Módulo de Incidentes
| Entidad | Atributos clave | Relaciones |
|---------|----------------|------------|
| **Incidente** | id, tipo, descripcion, fecha, estado, reportado_por | Independiente |
| **IncidenteBus** | id, incidente_id, bus_id, ubicacion, severidad | N:1 → Incidente, N:1 → Bus |
| **Foto** | id, url, descripcion, incidente_bus_id | N:1 → IncidenteBus |

### 💬 Módulo de Mensajería
| Entidad | Atributos clave | Relaciones |
|---------|----------------|------------|
| **Mensaje** | id, emisor_id, contenido, fecha_envio | N:1 → Persona (emisor) |
| **DestinatarioPersona** | id, mensaje_id, persona_id, leido | N:1 → Mensaje, N:1 → Persona |
| **DestinatarioGrupo** | id, mensaje_id, grupo_id | N:1 → Mensaje, N:1 → Grupo |
| **Grupo** | id, nombre, descripcion | N:N → Persona (via GrupoPersona) |
| **GrupoPersona** | id, grupo_id, persona_id, rol | N:1 → Grupo, N:1 → Persona |

---

## User Review Required

> [!IMPORTANT]
> **Base de datos:** El microservicio usará el mismo cluster MongoDB Atlas (`ClusterNarver`) pero con una base de datos separada llamada `db_negocio`. Confirma si este nombre te parece bien.

> [!IMPORTANT]
> **Puerto:** Propongo usar el puerto **3200** para `ms-negocio`, dado que `ms-security` usa 8080 y `ms-notificaciones` usa 5000. ¿Estás de acuerdo?

> [!IMPORTANT]
> **Referencia a usuarios:** En el diagrama, `Persona` se conecta con el sistema de seguridad. La estrategia propuesta es que `Persona` tenga un campo `user_id` que referencia al `_id` del usuario en `ms-security` (referencia cruzada entre bases de datos, no un DBRef de Mongoose). ¿Confirmas?

---

## Proposed Changes

### Estructura del proyecto

```
proyecto_buses/
├── ms-security/          (existente — Spring Boot)
├── ms-notificaciones/    (existente — Flask)
└── ms-negocio/           [NEW] — Node.js / Express
    ├── package.json
    ├── .env
    ├── .env.example
    ├── .gitignore
    ├── src/
    │   ├── index.js                    # Entry point
    │   ├── config/
    │   │   └── database.js             # Conexión Mongoose
    │   ├── models/
    │   │   ├── empresa.js
    │   │   ├── bus.js
    │   │   ├── gps.js
    │   │   ├── persona.js
    │   │   ├── conductor.js
    │   │   ├── ciudadano.js
    │   │   ├── direccion.js
    │   │   ├── ruta.js
    │   │   ├── nodo.js
    │   │   ├── paradero.js
    │   │   ├── programacion.js
    │   │   ├── turno.js
    │   │   ├── boleto.js
    │   │   ├── historial.js
    │   │   ├── metodoPago.js
    │   │   ├── metodoPagoCiudadano.js
    │   │   ├── incidente.js
    │   │   ├── incidenteBus.js
    │   │   ├── foto.js
    │   │   ├── mensaje.js
    │   │   ├── destinatarioPersona.js
    │   │   ├── destinatarioGrupo.js
    │   │   ├── grupo.js
    │   │   └── grupoPersona.js
    │   ├── validators/
    │   │   ├── empresaValidator.js
    │   │   ├── busValidator.js
    │   │   ├── gpsValidator.js
    │   │   ├── personaValidator.js
    │   │   ├── conductorValidator.js
    │   │   ├── ciudadanoValidator.js
    │   │   ├── direccionValidator.js
    │   │   ├── rutaValidator.js
    │   │   ├── nodoValidator.js
    │   │   ├── paraderoValidator.js
    │   │   ├── programacionValidator.js
    │   │   ├── turnoValidator.js
    │   │   ├── boletoValidator.js
    │   │   ├── historialValidator.js
    │   │   ├── metodoPagoValidator.js
    │   │   ├── metodoPagoCiudadanoValidator.js
    │   │   ├── incidenteValidator.js
    │   │   ├── incidenteBusValidator.js
    │   │   ├── fotoValidator.js
    │   │   ├── mensajeValidator.js
    │   │   ├── destinatarioPersonaValidator.js
    │   │   ├── destinatarioGrupoValidator.js
    │   │   ├── grupoValidator.js
    │   │   └── grupoPersonaValidator.js
    │   ├── controllers/
    │   │   ├── empresaController.js
    │   │   ├── busController.js
    │   │   ├── gpsController.js
    │   │   ├── personaController.js
    │   │   ├── conductorController.js
    │   │   ├── ciudadanoController.js
    │   │   ├── direccionController.js
    │   │   ├── rutaController.js
    │   │   ├── nodoController.js
    │   │   ├── paraderoController.js
    │   │   ├── programacionController.js
    │   │   ├── turnoController.js
    │   │   ├── boletoController.js
    │   │   ├── historialController.js
    │   │   ├── metodoPagoController.js
    │   │   ├── metodoPagoCiudadanoController.js
    │   │   ├── incidenteController.js
    │   │   ├── incidenteBusController.js
    │   │   ├── fotoController.js
    │   │   ├── mensajeController.js
    │   │   ├── destinatarioPersonaController.js
    │   │   ├── destinatarioGrupoController.js
    │   │   ├── grupoController.js
    │   │   └── grupoPersonaController.js
    │   └── routes/
    │       └── index.js                # Registro centralizado de rutas
    └── migrations/
        └── 001-initial-seed.js         # Seed inicial (métodos de pago, datos base)
```

---

### Componente 1: Configuración Base del Proyecto

#### [NEW] [package.json](file:///c:/Desarrollo%20backend/proyecto_buses/ms-negocio/package.json)
- Dependencias: `express`, `mongoose`, `express-validator`, `dotenv`, `cors`, `morgan`
- Dev: `nodemon`
- Scripts: `dev`, `start`, `migrate`

#### [NEW] [.env](file:///c:/Desarrollo%20backend/proyecto_buses/ms-negocio/.env)
```env
PORT=3200
MONGODB_URI=mongodb+srv://juannaranjo56321:Leondelcovid19.@clusternarver.pwnrxr8.mongodb.net/db_negocio?appName=ClusterNarver
SECURITY_SERVICE_URL=http://localhost:8080
```

#### [NEW] [src/index.js](file:///c:/Desarrollo%20backend/proyecto_buses/ms-negocio/src/index.js)
- Inicialización de Express, middlewares (cors, json, morgan)
- Conexión a MongoDB
- Registro de rutas

#### [NEW] [src/config/database.js](file:///c:/Desarrollo%20backend/proyecto_buses/ms-negocio/src/config/database.js)
- Conexión Mongoose al cluster con la BD `db_negocio`

---

### Componente 2: Modelos Mongoose (24 modelos)

Cada modelo incluirá:
- Schema con tipos, required, enum, default, ref (para relaciones)
- Timestamps automáticos (`createdAt`, `updatedAt`)
- Índices para campos de búsqueda frecuente
- Relaciones implementadas con `mongoose.Schema.Types.ObjectId` + `ref`

**Ejemplo de modelo con relación (Bus):**
```javascript
const busSchema = new mongoose.Schema({
    placa:       { type: String, required: true, unique: true },
    modelo:      { type: String, required: true },
    capacidad:   { type: Number, required: true },
    estado:      { type: String, enum: ['activo', 'inactivo', 'mantenimiento'], default: 'activo' },
    empresa_id:  { type: mongoose.Schema.Types.ObjectId, ref: 'Empresa', required: true },
}, { timestamps: true });
```

**Herencia Persona → Conductor/Ciudadano:** Se usará el patrón de discriminadores de Mongoose, donde `Persona` es el modelo base y `Conductor`/`Ciudadano` heredan sus campos.

---

### Componente 3: Validadores (24 validadores)

Cada validador usará `express-validator` con reglas para:
- Campos requeridos (`notEmpty`)
- Tipos de datos (`isString`, `isNumeric`, `isMongoId`)
- Formatos (`isEmail`, `isISO8601`, `matches` para placa)
- Rangos (`isInt({ min, max })`)
- Enums (`isIn([...])`)
- IDs de referencia válidos (`isMongoId`)

**Ejemplo (busValidator.js):**
```javascript
const { body } = require('express-validator');
module.exports = {
    create: [
        body('placa').notEmpty().matches(/^[A-Z]{3}-\d{3}$/),
        body('modelo').notEmpty().isString(),
        body('capacidad').isInt({ min: 1, max: 100 }),
        body('estado').optional().isIn(['activo', 'inactivo', 'mantenimiento']),
        body('empresa_id').notEmpty().isMongoId(),
    ],
    update: [
        body('placa').optional().matches(/^[A-Z]{3}-\d{3}$/),
        body('modelo').optional().isString(),
        // ...
    ]
};
```

---

### Componente 4: Controladores (24 controladores)

Cada controlador implementará operaciones **CRUD** estándar:
- `getAll` — Listar todos (con populate de relaciones)
- `getById` — Obtener por ID
- `create` — Crear nuevo registro
- `update` — Actualizar por ID
- `delete` — Eliminar por ID

Algunos controladores tendrán endpoints adicionales según el caso de uso:
- **BusController:** `getByEmpresa`, `updateEstado`
- **RutaController:** `getNodosDeRuta`, `getParaderosDeRuta`
- **BoletoController:** `getByCiudadano`, `getByProgramacion`
- **ConductorController:** `getTurnos`, `getRutaAsignada`
- **MensajeController:** `enviarMensaje`, `getByDestinatario`
- **IncidenteController:** `reportar`, `getByBus`

---

### Componente 5: Rutas Express

#### [NEW] [src/routes/index.js](file:///c:/Desarrollo%20backend/proyecto_buses/ms-negocio/src/routes/index.js)
Registro centralizado de todas las rutas:

| Prefijo | Controlador | Operaciones |
|---------|-------------|-------------|
| `/api/empresas` | empresaController | CRUD |
| `/api/buses` | busController | CRUD + getByEmpresa |
| `/api/gps` | gpsController | CRUD |
| `/api/personas` | personaController | CRUD |
| `/api/conductores` | conductorController | CRUD + getTurnos |
| `/api/ciudadanos` | ciudadanoController | CRUD |
| `/api/direcciones` | direccionController | CRUD |
| `/api/rutas` | rutaController | CRUD + getNodos + getParaderos |
| `/api/nodos` | nodoController | CRUD |
| `/api/paraderos` | paraderoController | CRUD |
| `/api/programaciones` | programacionController | CRUD |
| `/api/turnos` | turnoController | CRUD |
| `/api/boletos` | boletoController | CRUD + getByCiudadano |
| `/api/historial` | historialController | CRUD |
| `/api/metodos-pago` | metodoPagoController | CRUD |
| `/api/metodo-pago-ciudadano` | metodoPagoCiudadanoController | CRUD |
| `/api/incidentes` | incidenteController | CRUD |
| `/api/incidentes-bus` | incidenteBusController | CRUD |
| `/api/fotos` | fotoController | CRUD |
| `/api/mensajes` | mensajeController | CRUD + enviar |
| `/api/destinatario-persona` | destinatarioPersonaController | CRUD |
| `/api/destinatario-grupo` | destinatarioGrupoController | CRUD |
| `/api/grupos` | grupoController | CRUD |
| `/api/grupo-persona` | grupoPersonaController | CRUD |

---

### Componente 6: Migraciones / Seeds

#### [NEW] [migrations/001-initial-seed.js](file:///c:/Desarrollo%20backend/proyecto_buses/ms-negocio/migrations/001-initial-seed.js)
- Seed de métodos de pago predeterminados (Efectivo, Tarjeta Crédito, Tarjeta Débito, PSE, Nequi)
- Seed de tipos de incidentes base
- Datos de ejemplo para desarrollo

---

### Componente 7: Actualización del README

#### [MODIFY] [README.md](file:///c:/Desarrollo%20backend/proyecto_buses/README.md)
- Agregar documentación de `ms-negocio` al README del monorepo
- Incluir tabla de endpoints, instrucciones de instalación y configuración

---

## Plan de Trabajo — Tercera Entrega

### Integrante: Juan Sebastian Naranjo Verdugo

| # | Actividad | Tipo | Prioridad | Estado |
|---|-----------|------|-----------|--------|
| 1 | Configuración del proyecto Node.js (package.json, Express, Mongoose, .env) | Setup | 🔴 Alta | ⬜ Pendiente |
| 2 | Modelo + Validador + Controlador: **Empresa** | Backend | 🔴 Alta | ⬜ Pendiente |
| 3 | Modelo + Validador + Controlador: **Bus** | Backend | 🔴 Alta | ⬜ Pendiente |
| 4 | Modelo + Validador + Controlador: **GPS** | Backend | 🔴 Alta | ⬜ Pendiente |
| 5 | Modelo + Validador + Controlador: **Persona** (base) | Backend | 🔴 Alta | ⬜ Pendiente |
| 6 | Modelo + Validador + Controlador: **Conductor** | Backend | 🔴 Alta | ⬜ Pendiente |
| 7 | Modelo + Validador + Controlador: **Ciudadano** | Backend | 🟡 Media | ⬜ Pendiente |
| 8 | Modelo + Validador + Controlador: **Dirección** | Backend | 🟡 Media | ⬜ Pendiente |
| 9 | Modelo + Validador + Controlador: **Ruta** | Backend | 🔴 Alta | ⬜ Pendiente |
| 10 | Modelo + Validador + Controlador: **Nodo** | Backend | 🔴 Alta | ⬜ Pendiente |
| 11 | Modelo + Validador + Controlador: **Paradero** | Backend | 🔴 Alta | ⬜ Pendiente |
| 12 | Modelo + Validador + Controlador: **Programación** | Backend | 🔴 Alta | ⬜ Pendiente |
| 13 | Modelo + Validador + Controlador: **Turno** | Backend | 🟡 Media | ⬜ Pendiente |
| 14 | Modelo + Validador + Controlador: **Boleto** | Backend | 🔴 Alta | ⬜ Pendiente |
| 15 | Modelo + Validador + Controlador: **Historial** | Backend | 🟡 Media | ⬜ Pendiente |
| 16 | Modelo + Validador + Controlador: **MetodoPago** | Backend | 🟡 Media | ⬜ Pendiente |
| 17 | Modelo + Validador + Controlador: **MetodoPagoCiudadano** | Backend | 🟡 Media | ⬜ Pendiente |
| 18 | Modelo + Validador + Controlador: **Incidente** | Backend | 🟡 Media | ⬜ Pendiente |
| 19 | Modelo + Validador + Controlador: **IncidenteBus** | Backend | 🟡 Media | ⬜ Pendiente |
| 20 | Modelo + Validador + Controlador: **Foto** | Backend | 🟢 Baja | ⬜ Pendiente |
| 21 | Modelo + Validador + Controlador: **Mensaje** | Backend | 🟡 Media | ⬜ Pendiente |
| 22 | Modelo + Validador + Controlador: **DestinatarioPersona** | Backend | 🟡 Media | ⬜ Pendiente |
| 23 | Modelo + Validador + Controlador: **DestinatarioGrupo** | Backend | 🟢 Baja | ⬜ Pendiente |
| 24 | Modelo + Validador + Controlador: **Grupo** | Backend | 🟡 Media | ⬜ Pendiente |
| 25 | Modelo + Validador + Controlador: **GrupoPersona** | Backend | 🟡 Media | ⬜ Pendiente |
| 26 | Migración / Seed de datos iniciales | Backend | 🟡 Media | ⬜ Pendiente |
| 27 | Registro centralizado de rutas Express | Backend | 🔴 Alta | ⬜ Pendiente |
| 28 | Pruebas de endpoints con herramienta (Postman/Thunder Client) | Testing | 🔴 Alta | ⬜ Pendiente |
| 29 | Actualización de README del monorepo | Docs | 🟢 Baja | ⬜ Pendiente |
| 30 | Frontend: HU-ENTR-2-001 a HU-ENTR-2-016 | Frontend | 🔴 Alta | ⬜ Pendiente (esperando código) |

---

## Verification Plan

### Automated Tests
- Ejecutar `npm run dev` y verificar conexión exitosa a MongoDB Atlas
- Probar cada endpoint CRUD con Thunder Client o Postman
- Verificar que las validaciones rechacen datos inválidos (400 Bad Request)
- Verificar que las relaciones populate funcionen correctamente

### Manual Verification
- Verificar en MongoDB Atlas que la BD `db_negocio` se cree con todas las colecciones
- Confirmar que los seeds se ejecuten correctamente
- Probar flujos completos: crear empresa → crear bus → asignar GPS → crear programación
