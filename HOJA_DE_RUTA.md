# Hoja de Ruta de Implementación - Sistema de Buses Inteligentes

## 📅 Phases & Milestones

```
┌─────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│  FASE 1     │   FASE 2     │   FASE 3     │   FASE 4     │   FASE 5     │
│  Seguridad  │  Negocio     │  Viajes      │  Tiempo Real │  Avanzado    │
│  (Semanas   │  (Semanas    │  (Semanas    │  (Semanas    │  (Semanas    │
│   1-3)      │   4-7)       │   8-11)      │   12-15)     │   16+)       │
└─────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## 🔐 FASE 1: SEGURIDAD BASE (Semanas 1-3)

### Sprint 1.1: Roles y Permisos (Semana 1)

#### Modelos y Persistencia
- [ ] Crear modelo `Role.java`
  - `id`, `name`, `description`, `dateCreated`, `dateUpdated`
- [ ] Crear modelo `Permission.java`
  - `id`, `action` (CREATE, READ, UPDATE, DELETE), `module` (USERS, BUSES, ROUTES, etc)
- [ ] Crear modelo `UserRole.java` (relación N:N)
  - `userId`, `roleId`, `dateAssigned`
- [ ] Interfaz `RoleRepository extends MongoRepository<Role, String>`
- [ ] Interfaz `PermissionRepository extends MongoRepository<Permission, String>`
- [ ] Interfaz `UserRoleRepository extends MongoRepository<UserRole, String>`

#### Servicios
- [ ] Crear `RoleService.java` con métodos:
  - `createRole(name, description, permissions): Role`
  - `deleteRole(roleId): void` (validar que no hay usuarios)
  - `updateRolePermissions(roleId, permissions): Role`
  - `findAllRoles(): List<Role>`
- [ ] Crear `PermissionService.java` con métodos:
  - `getAllPermissions(): List<Permission>`
  - `createPermission(action, module): Permission`
- [ ] Actualizar `AuthService`:
  - Método `assignRoleToUser(userId, roleId): void`
  - Método `getUserRoles(userId): List<Role>`
  - Método `removeRoleFromUser(userId, roleId): void`

#### Controladores
- [ ] Crear `RoleController.java`:
  - `POST /api/roles` - Crear rol
  - `GET /api/roles` - Listar todos
  - `GET /api/roles/:id` - Detalles
  - `PUT /api/roles/:id` - Actualizar
  - `DELETE /api/roles/:id` - Eliminar
  - `PUT /api/roles/:id/permissions` - Actualizar permisos
- [ ] Crear `PermissionController.java`:
  - `GET /api/permissions` - Listar todos

#### Seguridad
- [ ] Implementar `RoleBasedAccessControl.java` (security util)
- [ ] Actualizar `SecurityConfig.java`:
  - Configurar acceso basado en roles
  - Anotar endpoints con `@PreAuthorize("hasRole('ADMIN')")`

---

### Sprint 1.2: Two-Factor Authentication (Semana 2)

#### Modelos
- [ ] Crear modelo `TwoFactorAuth.java`
  - `userId`, `code` (6 dígitos), `expiresAt`, `attempts`, `isActive`

#### Servicios
- [ ] Crear `TwoFactorAuthService.java`:
  - `generateCode(userId): String` (código 6 dígitos, válido 5 minutos)
  - `verifyCode(userId, code): boolean`
  - `invalidateCode(userId): void`
  - `getRemainingAttempts(userId): int`

#### Controladores
- [ ] Actualizar `AuthController`:
  - `POST /api/auth/2fa/verify` - Verificar código
  - `POST /api/auth/2fa/resend` - Reenviar código

#### Flujo
- [ ] Después de login exitoso:
  1. Generar código 2FA
  2. Enviar email con código (ms-notificaciones)
  3. Mostrar pantalla de verificación en frontend
  4. Validar código con máximo 3 intentos
  5. Si válido: generar JWT token

#### ms-notificaciones
- [ ] Crear función `send_2fa_code(email, code)`
- [ ] Template email: `2fa-code.html`

---

### Sprint 1.3: reCAPTCHA y Recuperación de Contraseña (Semana 3)

#### reCAPTCHA
- [ ] Obtener claves Google reCAPTCHA v3 en console.cloud.google.com
- [ ] Guardar en `application.properties`:
  ```properties
  recaptcha.secret=<secret-key>
  recaptcha.v3.threshold=0.5
  ```
- [ ] Crear `RecaptchaService.java`:
  - `verify(token): boolean`
  - Llamar a Google API para validar
- [ ] Actualizar `AuthController`:
  - Validar reCAPTCHA en `/api/auth/login`
  - Validar reCAPTCHA en `/api/auth/forgot-password`

#### Recuperación de Contraseña
- [ ] Crear modelo `PasswordResetToken.java`
  - `userId`, `token` (UUID), `expiresAt`, `isUsed`
- [ ] Crear `PasswordResetTokenRepository`
- [ ] Crear `PasswordResetService.java`:
  - `createResetToken(email): token` (válido 30 minutos)
  - `validateToken(token): boolean`
  - `resetPassword(token, newPassword): boolean`
- [ ] Actualizar `AuthController`:
  - `POST /api/auth/forgot-password` - Solicitar reset
  - `POST /api/auth/reset-password` - Ejecutar reset
  - Ambos con reCAPTCHA
- [ ] ms-notificaciones:
  - Función `send_password_reset_email(email, resetLink)`
  - Template: `password-reset.html`

#### Frontend (Angular)
- [ ] Componente `PasswordResetComponent`
  - Formulario con email
  - Mostrar mensaje genérico: "Si el email existe..."
  - reCAPTCHA
- [ ] Componente para ingresar nueva contraseña
  - Validar formato (8+, mayús, minús, número, especial)
  - Link desde email

---

## 🚗 FASE 2: MODELO DE NEGOCIO (Semanas 4-7)

### Sprint 2.1: Empresas y Buses (Semanas 4-5)

#### Modelos
- [ ] Crear `Empresa.java`
  - `id`, `nombre`, `representante_id` (ref: User), `contacto`, `direccion`, `isActive`, `dateCreated`
- [ ] Crear `Bus.java`
  - `id`, `placa` (unique), `modelo`, `capacidad`, `empresa_id`, `estado` (ACTIVO, MANTENIMIENTO, FUERA_SERVICIO)
  - `gps_id` (identificador único del dispositivo GPS), `dateCreated`
- [ ] Crear `GPSData.java` (para rastreo)
  - `busId`, `latitud`, `longitud`, `timestamp`, `velocidad`

#### Repositorios
- [ ] `EmpresaRepository`
- [ ] `BusRepository`
- [ ] `GPSDataRepository`

#### Servicios
- [ ] `EmpresaService`:
  - `createEmpresa(nombre, representante, contacto): Empresa`
  - `updateEmpresa(id, data): Empresa`
  - `listEmpresas(): List<Empresa>`
  - `deleteEmpresa(id): void`
- [ ] `BusService`:
  - `createBus(placa, modelo, capacidad, empresa_id): Bus`
  - `updateBus(id, data): Bus`
  - `listBusesByEmpresa(empresa_id): List<Bus>`
  - `updateBusState(id, state): Bus`
  - `deletePhysicalBus(id): void` (soft delete)

#### Controladores
- [ ] `EmpresaController`:
  - `POST /api/empresas` (solo admin)
  - `GET /api/empresas`
  - `GET /api/empresas/:id`
  - `PUT /api/empresas/:id`
  - `DELETE /api/empresas/:id`
- [ ] `BusController`:
  - `POST /api/buses` (admin o empresa representante)
  - `GET /api/buses`
  - `GET /api/buses/:id`
  - `PUT /api/buses/:id`
  - `DELETE /api/buses/:id`
  - `GET /api/buses/:id/ubicacion` (GPS actual)
  - `GET /api/buses/empresa/:empresa_id`

#### Frontend
- [ ] Componente `ListaBuses`
- [ ] Componente `CrearBus`
- [ ] Componente `EditarBus`
- [ ] Componente `DetallesBus` (con GPS en mapa)

---

### Sprint 2.2: Rutas y Paraderos (Semana 6)

#### Modelos
- [ ] Crear `Paradero.java`
  - `id`, `nombre`, `ubicacion` (GeoJSON: {lat, lon}), `clasificacion` (PRINCIPAL, INTERMEDIA, TERMINAL)
  - `dateCreated`
- [ ] Crear `Ruta.java`
  - `id`, `nombre`, `descripcion`, `tarifa`, `empresa_id`
  - `paraderos_ordenados` (Array de {paradero_id, orden})
  - `dateCreated`

#### Índices MongoDB
- [ ] Crear índice geoespacial en `Paradero.ubicacion`
- [ ] Crear índice en `Ruta.empresa_id`

#### Repositorios
- [ ] `ParaderoRepository`
- [ ] `RutaRepository`

#### Servicios
- [ ] `ParaderoService`:
  - `createParadero(nombre, ubicacion, clasificacion): Paradero`
  - `listParaderos(): List<Paradero>`
  - `findNearby(lat, lon, distance): List<Paradero>` (geospatial query)
  - `updateParadero(id, data): Paradero`
- [ ] `RutaService`:
  - `createRuta(nombre, descripcion, tarifa, empresa_id): Ruta`
  - `addParaderoARuta(ruta_id, paradero_id, orden): Ruta`
  - `removeParaderoDeRuta(ruta_id, paradero_id): Ruta`
  - `listRutas(): List<Ruta>`
  - `getRutasByEmpresa(empresa_id): List<Ruta>`
  - `obtenerParaderosOrdenados(ruta_id): List<Paradero>`

#### Controladores
- [ ] `ParaderoController`:
  - `POST /api/paraderos`
  - `GET /api/paraderos`
  - `GET /api/paraderos?lat=X&lon=Y&distance=Z` (busca cercanos)
  - `PUT /api/paraderos/:id`
- [ ] `RutaController`:
  - `POST /api/rutas` (crear ruta vacía)
  - `GET /api/rutas`
  - `GET /api/rutas/:id`
  - `PUT /api/rutas/:id`
  - `POST /api/rutas/:id/paraderos` (agregar paradero)
  - `DELETE /api/rutas/:id/paraderos/:paradero_id` (quitar paradero)
  - `GET /api/rutas/:id/paraderos` (obtener secuencia)
  - `GET /api/rutas/empresa/:empresa_id`

#### Frontend
- [ ] Componente `ListaRutas` con tabla
- [ ] Componente `CrearRuta` con:
  - Formulario: nombre, descripción, tarifa
  - Selector de paraderos con orden (drag-and-drop)
  - Preview en mapa
- [ ] Componente `VisualizarRuta` (Mapa Google con paraderos conectados)
- [ ] Componente `CrearParadero` (Mapa con marker)

---

### Sprint 2.3: Programaciones y Turnos (Semana 7)

#### Modelos
- [ ] Crear `Programacion.java`
  - `id`, `bus_id`, `ruta_id`, `fecha`, `hora_inicio`, `conductor_id` (nullable inicialmente)
  - `estado` (PROGRAMADO, EN_VIAJE, COMPLETADO, CANCELADO), `dateCreated`
- [ ] Crear `Turno.java`
  - `id`, `conductor_id`, `bus_id`, `fecha`, `hora_inicio`, `hora_fin`
  - `estado` (PROGRAMADO, EN_CURSO, COMPLETADO, CANCELADO), `dateCreated`

#### Repositorios
- [ ] `ProgramacionRepository` con queries:
  - `findByBusAndFecha(busId, fecha): List<Programacion>`
  - `findByRutaAndFecha(rutaId, fecha): List<Programacion>`
  - `findByEstado(estado): List<Programacion>` (filtrar activas)
- [ ] `TurnoRepository` con queries:
  - `findByConductorAndFecha(conductorId, fecha): List<Turno>`
  - `findByBusAndFecha(busId, fecha): List<Turno>`

#### Servicios
- [ ] `ProgramacionService`:
  - `createProgramacion(bus_id, ruta_id, fecha, hora): Programacion`
  - `assignConductor(programacion_id, conductor_id): Programacion`
  - `listProgramacionesByFecha(fecha): List<Programacion>`
  - `updateProgramacion(id, data): Programacion`
  - `cancelProgramacion(id): Programacion`
  - `getProgramacionesActuales(): List<Programacion>` (ahora)
  - `verfifyDisponibilidad(bus_id, fecha, hora): boolean`
- [ ] `TurnoService`:
  - `createTurno(conductor_id, bus_id, fecha, hora_inicio, hora_fin): Turno`
  - `listTurnosConductor(conductor_id, fecha): List<Turno>`
  - `listTurnosBus(bus_id, fecha): List<Turno>`
  - `updateTurno(id, data): Turno`

#### Controladores
- [ ] `ProgramacionController`:
  - `POST /api/programaciones` (crear)
  - `GET /api/programaciones?fecha=X` (listar por fecha)
  - `GET /api/programaciones/:id`
  - `PUT /api/programaciones/:id` (editar)
  - `DELETE /api/programaciones/:id` (cancelar)
  - `POST /api/programaciones/:id/asignar-conductor`
  - `GET /api/programaciones/activas` (en este momento)
  - `GET /api/buses/:bus_id/siguiente` (próxima programación)
- [ ] `TurnoController`:
  - `POST /api/turnos` (crear)
  - `GET /api/turnos?conductor_id=X&fecha=Y`
  - `GET /api/turnos/:id`
  - `PUT /api/turnos/:id`
  - `DELETE /api/turnos/:id`

#### Frontend
- [ ] Componente `MisProximos Turnos` (para conductor)
- [ ] Componente `CrearProgramacion` (admin/empresa)
- [ ] Componente `CalendarioProgramaciones` (vista calendario)

---

## 🎫 FASE 3: SISTEMA DE VIAJES (Semanas 8-11)

### Sprint 3.1: Métodos de Pago (Semana 8)

#### Modelos
- [ ] Crear `MetodoPago.java`
  - `id`, `ciudadano_id`, `tipo` (TARJETA_DEBITO, TARJETA_CREDITO, RECARGABLE, BILLETERA_MOVIL, EFECTIVO)
  - `numero_id` (encrypted), `estado` (ACTIVO, INACTIVO)
  - `saldo` (Decimal128, solo si aplica), `dateCreated`

#### Servicios
- [ ] `MetodoPagoService`:
  - `registerPaymentMethod(ciudadano_id, tipo, numero): MetodoPago`
  - `listMetodosPago(ciudadano_id): List<MetodoPago>`
  - `consultarSaldo(metodo_pago_id): Decimal128`
  - `recargar(metodo_pago_id, monto): MetodoPago`
  - `deducirSaldo(metodo_pago_id, monto): boolean`
  - `desactivarMetodo(metodo_pago_id): MetodoPago` (soft delete)
  - Encriptación de `numero_id` (AES)

#### Controladores
- [ ] `MetodoPagoController`:
  - `GET /api/metodos-pago` (mis métodos)
  - `POST /api/metodos-pago` (registrar nuevo)
  - `PUT /api/metodos-pago/:id` (actualizar)
  - `DELETE /api/metodos-pago/:id` (desactivar)
  - `GET /api/metodos-pago/:id/saldo` (consultar saldo)
  - `POST /api/metodos-pago/:id/recargar` (recargar)

#### ms-notificaciones
- [ ] Crear función `send_recharge_confirmation(email, metodo, monto, nuevo_saldo)`

#### Frontend
- [ ] Componente `MisMetodosPago`
- [ ] Componente `RegistrarMetodoPago`
- [ ] Componente `ConsultarSaldo` (dashboard)
- [ ] Componente `RecargarSaldo` (modal)

---

### Sprint 3.2: Boletos y Validación (Semanas 9-10)

#### Modelos
- [ ] Crear `Boleto.java`:
  - `id`, `ciudadano_id`, `programacion_id`, `metodo_pago_id`
  - `costo`, `timestamp_validacion_inicio`, `timestamp_validacion_fin` (nullable)
  - `estado` (EN_VIAJE, COMPLETADO, CANCELADO), `dateCreated`
- [ ] Crear `ValidacionBoleto.java`:
  - `id`, `boleto_id`, `paradero_id`, `orden` (posición en ruta)
  - `timestamp`, `tipo_accion` (ABORDAJE, DESCENSO), `dateCreated`

#### Repositorios
- [ ] `BoletoRepository`:
  - `findByCiudadanoAndEstado(ciudadanoId, estado): List<Boleto>`
  - `findByProgramacion(programacionId): List<Boleto>` (pasajeros activos en bus)
- [ ] `ValidacionBoletoRepository`:
  - `findByBoleto(boletoId): List<ValidacionBoleto>` (ordenadas por timestamp)

#### Servicios
- [ ] `BoletoService`:
  - **Validar Inicio (Abordaje)**:
    ```
    validarInicio(ciudadano_id, programacion_id, metodo_pago_id):
      1. Buscar ciudadano
      2. Verificar método de pago existe
      3. Obtener costo de ruta
      4. Verificar saldo disponible
      5. Contar boletos activos (estado = EN_VIAJE)
      6. Verificar capacidad del bus
        - Si >= capacidad: throw CapacidadExcedidaException
      7. Deducir saldo de método de pago
      8. Crear boleto (estado = EN_VIAJE)
      9. Crear validación ABORDAJE en paradero_inicio
      10. Guardar en MongoDB
      11. Return Boleto
    ```
  - **Validar Salida (Descenso)**:
    ```
    validarSalida(boleto_id, paradero_id):
      1. Obtener boleto
      2. Verificar estado = EN_VIAJE
      3. Crear validación DESCENSO
      4. Cambiar estado a COMPLETADO
      5. Return Boleto
    ```
  - `getBoletosPorCiudadano(ciudadano_id): List<Boleto>`
  - `obtenerHistorialViajes(ciudadano_id): List<Boleto>`
  - `getPasajerosActivos(programacion_id): int` (contar boletos EN_VIAJE)
  - Métodos auxiliares: `verificarCapacidad()`, `verificarSaldo()`

- [ ] `ValidacionService`:
  - `getHistorialValidaciones(boleto_id): List<ValidacionBoleto>`
  - `reconstruirRuta(boleto_id): RutaReconstruida` (paraderos + timestamps)
  - `calcularTiempoViaje(boleto_id): Duration`

#### Controladores
- [ ] `BoletoController`:
  - `POST /api/boletos/validar-inicio` → call `BoletoService.validarInicio()`
    - Body: `{ciudadano_id, programacion_id, metodo_pago_id}`
    - Response: 200 con Boleto o 400 si rechazo (sin saldo, bus lleno, etc)
  - `POST /api/boletos/validar-salida` → call `BoletoService.validarSalida()`
    - Body: `{boleto_id, paradero_id}`
    - Response: 200 con Boleto completado
  - `GET /api/boletos/:id` (detalles boleto)
  - `GET /api/ciudadano/boletos` (mis boletos del usuario autenticado)
  - `GET /api/ciudadano/historial-viajes` (completo)
- [ ] `ValidacionController`:
  - `GET /api/validaciones/boleto/:boleto_id` (historial de validaciones)
  - `GET /api/validaciones/boleto/:boleto_id/ruta` (ruta reconstruida)

#### ms-notificaciones
- [ ] `send_travel_confirmation(email, boleto)`
- [ ] `send_balance_alert(email, metodo_pago, saldo_restante)`
- [ ] Templates: `travel-confirmation.html`, `balance-alert.html`

#### Frontend
- [ ] Componente `ValidarBoleto` (hardware + interfaz)
- [ ] Componente `MisViajes` - Historial
- [ ] Componente `DetalleViaje` - Mostrar ruta, tiempos, validaciones
- [ ] Componente `Capacidad del Bus` (indicador %)

#### Testing
- [ ] Unit tests para `BoletoService.validarInicio()` (casos de éxito, sin saldo, bus lleno, etc)
- [ ] Integration tests para endpoints POST validar-inicio/salida

---

### Sprint 3.3: Historial y Reportes Básicos (Semana 11)

#### Servicios
- [ ] `ReporteService` (básicos):
  - `reportePasajerosPorRuta(ruta_id, fecha): Map<Ruta, Integer>`
  - `reportePasajerosPorBus(bus_id, fecha): Integer`
  - `reporteIngresoPorRuta(ruta_id, fecha): Decimal128`
  - `reporteIngresoTotalEmpresa(empresa_id, fecha): Decimal128`

#### Controladores
- [ ] `ReporteController`:
  - `GET /api/reportes/pasajeros/ruta/:ruta_id?fecha=X`
  - `GET /api/reportes/pasajeros/bus/:bus_id?fecha=X`
  - `GET /api/reportes/ingresos/ruta/:ruta_id?fecha=X`
  - `GET /api/reportes/ingresos/empresa/:empresa_id?fecha=X`

---

## 🌍 FASE 4: TIEMPO REAL (Semanas 12-15)

### Sprint 4.1: GPS y WebSockets (Semanas 12-13)

#### Backend
- [ ] Crear `GPSService`:
  - Mock o integración real de GPS
  - `updateBusLocation(bus_id, lat, lon, velocidad): GPSData`
  - `getBusLocation(bus_id): GPSData`
  - `getHistorialUbicaciones(bus_id, desde, hasta): List<GPSData>`

- [ ] Crear WebSocket handlers:
  - `GPSWebSocketHandler` - maneja conexiones de rastreo
  - Salas: `bus-{bus_id}` → clientes suscritos reciben actualizaciones
  - Formato: `{busId, lat, lon, velocidad, timestamp, programacion_id}`

- [ ] Actualizar controladores:
  - `BusController.GET /api/buses/:id/ubicacion` → llamar GPSService
  - `GET /api/buses/:id/siguiente` con ETA estimado

#### Frontend (Angular)
- [ ] Servicio `GPSService`:
  - Conectar a WebSocket
  - Suscribirse a canal de bus
  - Emitir eventos cuando hay actualización

- [ ] Componente `RastreadoBus`:
  - Integración Google Maps API
  - Markers para:
    - Bus actual (rojo, con rotación)
    - Paraderos de la ruta (azul)
    - Paradero actual (resaltado)
  - Información: ubicación actual, velocidad, ETA
  - Zoom automático al bus
  - Lista de próximos paraderos

- [ ] Actualizar `AuthService` con endpoints de ubicación
- [ ] Guard para validar permisos (solo ver bus si es pasajero o admin)

#### Testing
- [ ] Tests para `GPSService`
- [ ] Tests para WebSocket (simulación)

---

### Sprint 4.2: Notificaciones en Tiempo Real (Semana 14)

#### Backend
- [ ] Crear `NotificacionService`:
  - `notifyDelay(programacion_id, delay_minutes): void`
  - `notifyNextStop(boleto_id, paradero_siguiente): void`
  - `notifyBusAtCapacity(programacion_id): void`
  - `notifyIncident(incidente_id): void`

- [ ] Crear WebSocket handlers:
  - `NotificationWebSocketHandler`
  - Salas: `notifications-{usuario_id}`
  - Eventos: delay, next-stop, capacity, incident

- [ ] Integración con ms-notificaciones:
  - Enviar también como email/SMS (opcional)

#### Frontend
- [ ] Servicio `NotificationService` (angular):
  - Conectar a WebSocket de notificaciones
  - Mostrar toasts/snackbars

- [ ] Componente `NotificationCenter`:
  - Toast en esquina
  - Sonido/vibración en mobile
  - Historial de notificaciones

```
Ejemplos de notificaciones:
- "Bus retrasado 12 minutos"
- "Bus está a 3 paradas, llegará en 8 minutos"
- "Bus está lleno, considera el siguiente"
- "Incidente reportado en Ruta Norte"
```

---

### Sprint 4.3: Optimizaciones de Tiempo Real (Semana 15)

- [ ] Caching de ubicaciones recientes (Redis, si escala)
- [ ] Límite de frecuencia de actualizaciones GPS (ej: cada 10 segundos)
- [ ] Compresión de datos en WebSocket
- [ ] Circuit breaker para GPS fallida

---

## 🎭 FASE 5: FUNCIONALIDADES AVANZADAS (Semanas 16+)

### Sprint 5.1: Incidentes

#### Modelos
- [ ] Crear `Incidente.java`:
  - `id`, `bus_id`, `descripcion`, `tipo` (ACCIDENTE, FALLA_MECANICA, CONGESTION, PROBLEMA_PASAJERO)
  - `timestamp`, `fotos` (Array<String> URLs), `estado` (REPORTADO, EN_INVESTIGACION, RESUELTO)
  - `conductor_id` (quien reporta), `dateCreated`

#### Servicios
- [ ] `IncidenteService`:
  - `reportarIncidente(bus_id, conductor_id, descripcion, tipo): Incidente`
  - `uploadFoto(incidente_id, file): Incidente` (guardar en cloud storage)
  - `listIncidentes(filtros): List<Incidente>`
  - `marcarResuelto(incidente_id): Incidente`

#### Controladores
- [ ] `IncidenteController`:
  - `POST /api/incidentes` (reportar)
  - `GET /api/incidentes` (listar)
  - `GET /api/incidentes/:id` (detalles)
  - `POST /api/incidentes/:id/fotos` (upload)
  - `PUT /api/incidentes/:id` (actualizar estado)

#### Frontend
- [ ] Componente `ReportarIncidente` (en app del conductor)

---

### Sprint 5.2: Mensajería Entre Usuarios

#### Modelos
- [ ] Crear `GrupoMensajeria.java`:
  - `id`, `nombre`, `descripcion`, `miembros` (Array<user_id>)
  - `creador_id`, `dateCreated`
- [ ] Crear `Mensaje.java`:
  - `id`, `contenido`, `emisor_id`, `destinatarios` (Array con estructura flexible)
  - `timestamp`, `dateCreated`

#### Servicios
- [ ] `MensajeService`:
  - `enviarMensajeIndividual(emisor_id, destinatario_id, contenido): Mensaje`
  - `enviarMensajeGrupo(emisor_id, grupo_id, contenido): Mensaje`
  - `crearGrupo(nombre, descripcion, miembros): GrupoMensajeria`

#### Controladores
- [ ] `MensajeController` y `GrupoController`

#### Frontend
- [ ] Componentes de mensajería

---

### Sprint 5.3: Reportes Avanzados y Analytics

- Analytics de movilidad
- Reportes de auditoría
- Dashboard ejecutivo

---

## 🔧 Tareas Transversales (Todas las fases)

### Documentación
- [ ] Documentar cada servicio con Javadoc
- [ ] README para cada microservicio
- [ ] API Documentation (Swagger/OpenAPI)

### Testing
- [ ] Unit tests (AuthService, BusService, etc)
- [ ] Integration tests (APIs)
- [ ] E2E tests (flujo completo de viaje)

### DevOps
- [ ] Docker Compose para desarrollo local
- [ ] GitHub Actions para CI
- [ ] Deployment guide a AWS/GCP

### Seguridad
- [ ] Validación de entrada en todos los endpoints
- [ ] Encriptación de datos sensibles
- [ ] Rate limiting en APIs
- [ ] CORS configuration

---

## 📊 Tabla Resumen de Tareas

| Fase | Sprint | Estimado | Estado | Notas |
|------|--------|----------|--------|-------|
| 1.1 | Roles y Permisos | 5 días | 🔄 NO INICIADO | Crítico |
| 1.2 | 2FA | 3 días | 🔄 NO INICIADO | Crítico |
| 1.3 | reCAPTCHA + Recovery | 3 días | 🔄 NO INICIADO | Importante |
| 2.1 | Buses y Empresas | 7 días | 🔄 NO INICIADO | Crítico |
| 2.2 | Rutas y Paraderos | 5 días | 🔄 NO INICIADO | Crítico |
| 2.3 | Programaciones | 4 días | 🔄 NO INICIADO | Crítico |
| 3.1 | Métodos de Pago | 4 días | 🔄 NO INICIADO | Crítico |
| 3.2 | Boletos y Validación | 7 días | 🔄 NO INICIADO | Crítico |
| 3.3 | Reportes Básicos | 3 días | 🔄 NO INICIADO | Importante |
| 4.1 | GPS y WebSockets | 6 días | 🔄 NO INICIADO | Importante |
| 4.2 | Notificaciones RT | 4 días | 🔄 NO INICIADO | Importante |
| 4.3 | Optimizaciones | 3 días | 🔄 NO INICIADO | Secundario |
| 5.1 | Incidentes | 4 días | 🔄 NO INICIADO | Secundario |
| 5.2 | Mensajería | 5 días | 🔄 NO INICIADO | Secundario |
| 5.3 | Analytics | 4 días | 🔄 NO INICIADO | Secundario |

---

## 🎯 Dependencias Entre Sprints

```
Fase 1 (Seguridad)
  │
  └──> Fase 2 (Negocio Base)
         │
         └──> Fase 3 (Sistema Viajes) ← CRÍTICO
                │
                └──> Fase 4 (Tiempo Real)
                       │
                       └──> Fase 5 (Avanzado)
```

**Ruta Crítica**: Seguridad → Buses/Rutas → Boletos/Validación → GPS

---

**Última actualización**: Marzo 19, 2026  
**Próxima revisión**: Después de completar Fase 1
