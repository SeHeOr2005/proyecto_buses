const { Router } = require('express');
const validate = require('../validators/validate');

// --- Controladores ---
const empresaCtrl           = require('../controllers/empresaController');
const busCtrl               = require('../controllers/busController');
const gpsCtrl               = require('../controllers/gpsController');
const personaCtrl           = require('../controllers/personaController');
const conductorCtrl         = require('../controllers/conductorController');
const ciudadanoCtrl         = require('../controllers/ciudadanoController');
const direccionCtrl         = require('../controllers/direccionController');
const rutaCtrl              = require('../controllers/rutaController');
const nodoCtrl              = require('../controllers/nodoController');
const paraderoCtrl          = require('../controllers/paraderoController');
const programacionCtrl      = require('../controllers/programacionController');
const turnoCtrl             = require('../controllers/turnoController');
const boletoCtrl            = require('../controllers/boletoController');
const historialCtrl         = require('../controllers/historialController');
const metodoPagoCtrl        = require('../controllers/metodoPagoController');
const metodoPagoCiudCtrl    = require('../controllers/metodoPagoCiudadanoController');
const incidenteCtrl         = require('../controllers/incidenteController');
const incidenteBusCtrl      = require('../controllers/incidenteBusController');
const fotoCtrl              = require('../controllers/fotoController');
const mensajeCtrl           = require('../controllers/mensajeController');
const destPersonaCtrl       = require('../controllers/destinatarioPersonaController');
const destGrupoCtrl         = require('../controllers/destinatarioGrupoController');
const grupoCtrl             = require('../controllers/grupoController');
const grupoPersonaCtrl      = require('../controllers/grupoPersonaController');

// --- Validadores ---
const empresaVal            = require('../validators/empresaValidator');
const busVal                = require('../validators/busValidator');
const gpsVal                = require('../validators/gpsValidator');
const personaVal            = require('../validators/personaValidator');
const conductorVal          = require('../validators/conductorValidator');
const ciudadanoVal          = require('../validators/ciudadanoValidator');
const direccionVal          = require('../validators/direccionValidator');
const rutaVal               = require('../validators/rutaValidator');
const nodoVal               = require('../validators/nodoValidator');
const paraderoVal           = require('../validators/paraderoValidator');
const programacionVal       = require('../validators/programacionValidator');
const turnoVal              = require('../validators/turnoValidator');
const boletoVal             = require('../validators/boletoValidator');
const historialVal          = require('../validators/historialValidator');
const metodoPagoVal         = require('../validators/metodoPagoValidator');
const metodoPagoCiudVal     = require('../validators/metodoPagoCiudadanoValidator');
const incidenteVal          = require('../validators/incidenteValidator');
const incidenteBusVal       = require('../validators/incidenteBusValidator');
const fotoVal               = require('../validators/fotoValidator');
const mensajeVal            = require('../validators/mensajeValidator');
const destPersonaVal        = require('../validators/destinatarioPersonaValidator');
const destGrupoVal          = require('../validators/destinatarioGrupoValidator');
const grupoVal              = require('../validators/grupoValidator');
const grupoPersonaVal       = require('../validators/grupoPersonaValidator');

const router = Router();

// ======================== Empresas ========================
router.get('/empresas',        empresaCtrl.getAll);
router.get('/empresas/:id',    empresaCtrl.getById);
router.post('/empresas',       empresaVal.create, validate, empresaCtrl.create);
router.put('/empresas/:id',    empresaVal.update, validate, empresaCtrl.update);
router.delete('/empresas/:id', empresaCtrl.delete);

// ======================== Buses ========================
router.get('/buses',                      busCtrl.getAll);
router.get('/buses/:id',                  busCtrl.getById);
router.get('/buses/empresa/:empresaId',   busCtrl.getByEmpresa);
router.post('/buses',                     busVal.create, validate, busCtrl.create);
router.put('/buses/:id',                  busVal.update, validate, busCtrl.update);
router.delete('/buses/:id',               busCtrl.delete);

// ======================== GPS ========================
router.get('/gps',                  gpsCtrl.getAll);
router.get('/gps/:id',              gpsCtrl.getById);
router.get('/gps/bus/:busId',       gpsCtrl.getByBus);
router.post('/gps',                 gpsVal.create, validate, gpsCtrl.create);
router.put('/gps/:id',              gpsVal.update, validate, gpsCtrl.update);
router.delete('/gps/:id',           gpsCtrl.delete);

// ======================== Personas ========================
router.get('/personas',        personaCtrl.getAll);
router.get('/personas/:id',    personaCtrl.getById);
router.post('/personas',       personaVal.create, validate, personaCtrl.create);
router.put('/personas/:id',    personaVal.update, validate, personaCtrl.update);
router.delete('/personas/:id', personaCtrl.delete);

// ======================== Conductores ========================
router.get('/conductores',                    conductorCtrl.getAll);
router.get('/conductores/:id',                conductorCtrl.getById);
router.get('/conductores/:id/turnos',         conductorCtrl.getTurnos);
router.post('/conductores',                   conductorVal.create, validate, conductorCtrl.create);
router.put('/conductores/:id',                conductorVal.update, validate, conductorCtrl.update);
router.delete('/conductores/:id',             conductorCtrl.delete);

// ======================== Ciudadanos ========================
router.get('/ciudadanos',        ciudadanoCtrl.getAll);
router.get('/ciudadanos/:id',    ciudadanoCtrl.getById);
router.post('/ciudadanos',       ciudadanoVal.create, validate, ciudadanoCtrl.create);
router.put('/ciudadanos/:id',    ciudadanoVal.update, validate, ciudadanoCtrl.update);
router.delete('/ciudadanos/:id', ciudadanoCtrl.delete);

// ======================== Direcciones ========================
router.get('/direcciones',                              direccionCtrl.getAll);
router.get('/direcciones/:id',                          direccionCtrl.getById);
router.get('/direcciones/ciudadano/:ciudadanoId',       direccionCtrl.getByCiudadano);
router.post('/direcciones',                             direccionVal.create, validate, direccionCtrl.create);
router.put('/direcciones/:id',                          direccionVal.update, validate, direccionCtrl.update);
router.delete('/direcciones/:id',                       direccionCtrl.delete);

// ======================== Rutas ========================
router.get('/rutas',                   rutaCtrl.getAll);
router.get('/rutas/:id',               rutaCtrl.getById);
router.get('/rutas/:id/nodos',         rutaCtrl.getNodos);
router.get('/rutas/:id/paraderos',     rutaCtrl.getParaderos);
router.post('/rutas',                  rutaVal.create, validate, rutaCtrl.create);
router.put('/rutas/:id',               rutaVal.update, validate, rutaCtrl.update);
router.delete('/rutas/:id',            rutaCtrl.delete);

// ======================== Nodos ========================
router.get('/nodos',        nodoCtrl.getAll);
router.get('/nodos/:id',    nodoCtrl.getById);
router.post('/nodos',       nodoVal.create, validate, nodoCtrl.create);
router.put('/nodos/:id',    nodoVal.update, validate, nodoCtrl.update);
router.delete('/nodos/:id', nodoCtrl.delete);

// ======================== Paraderos ========================
router.get('/paraderos',        paraderoCtrl.getAll);
router.get('/paraderos/cercanos', paraderoCtrl.getCercanos);
router.get('/paraderos/:id',    paraderoCtrl.getById);
router.post('/paraderos',       paraderoVal.create, validate, paraderoCtrl.create);
router.put('/paraderos/:id',    paraderoVal.update, validate, paraderoCtrl.update);
router.delete('/paraderos/:id', paraderoCtrl.delete);

// ======================== Programaciones ========================
router.get('/programaciones',        programacionCtrl.getAll);
router.get('/programaciones/:id',    programacionCtrl.getById);
router.post('/programaciones',       programacionVal.create, validate, programacionCtrl.create);
router.put('/programaciones/:id',    programacionVal.update, validate, programacionCtrl.update);
router.delete('/programaciones/:id', programacionCtrl.delete);

// ======================== Turnos ========================
router.get('/turnos',        turnoCtrl.getAll);
router.get('/turnos/:id',    turnoCtrl.getById);
router.post('/turnos',       turnoVal.create, validate, turnoCtrl.create);
router.put('/turnos/:id',    turnoVal.update, validate, turnoCtrl.update);
router.delete('/turnos/:id', turnoCtrl.delete);

// ======================== Boletos ========================
router.get('/boletos',                              boletoCtrl.getAll);
router.get('/boletos/:id',                          boletoCtrl.getById);
router.get('/boletos/ciudadano/:ciudadanoId',       boletoCtrl.getByCiudadano);
router.post('/boletos',                             boletoVal.create, validate, boletoCtrl.create);
router.put('/boletos/:id',                          boletoVal.update, validate, boletoCtrl.update);
router.delete('/boletos/:id',                       boletoCtrl.delete);

// ======================== Historial ========================
router.get('/historial',                        historialCtrl.getAll);
router.get('/historial/:id',                    historialCtrl.getById);
router.get('/historial/boleto/:boletoId',       historialCtrl.getByBoleto);
router.post('/historial',                       historialVal.create, validate, historialCtrl.create);
router.put('/historial/:id',                    historialVal.update, validate, historialCtrl.update);
router.delete('/historial/:id',                 historialCtrl.delete);

// ======================== Métodos de Pago ========================
router.get('/metodos-pago',        metodoPagoCtrl.getAll);
router.get('/metodos-pago/:id',    metodoPagoCtrl.getById);
router.post('/metodos-pago',       metodoPagoVal.create, validate, metodoPagoCtrl.create);
router.put('/metodos-pago/:id',    metodoPagoVal.update, validate, metodoPagoCtrl.update);
router.delete('/metodos-pago/:id', metodoPagoCtrl.delete);

// ======================== MetodoPago-Ciudadano ========================
router.get('/metodo-pago-ciudadano',                              metodoPagoCiudCtrl.getAll);
router.get('/metodo-pago-ciudadano/:id',                          metodoPagoCiudCtrl.getById);
router.get('/metodo-pago-ciudadano/ciudadano/:ciudadanoId',       metodoPagoCiudCtrl.getByCiudadano);
router.post('/metodo-pago-ciudadano',                             metodoPagoCiudVal.create, validate, metodoPagoCiudCtrl.create);
router.put('/metodo-pago-ciudadano/:id',                          metodoPagoCiudVal.update, validate, metodoPagoCiudCtrl.update);
router.delete('/metodo-pago-ciudadano/:id',                       metodoPagoCiudCtrl.delete);

// ======================== Incidentes ========================
router.get('/incidentes',        incidenteCtrl.getAll);
router.get('/incidentes/:id',    incidenteCtrl.getById);
router.post('/incidentes',       incidenteVal.create, validate, incidenteCtrl.create);
router.put('/incidentes/:id',    incidenteVal.update, validate, incidenteCtrl.update);
router.delete('/incidentes/:id', incidenteCtrl.delete);

// ======================== Incidentes-Bus ========================
router.get('/incidentes-bus',                    incidenteBusCtrl.getAll);
router.get('/incidentes-bus/:id',                incidenteBusCtrl.getById);
router.get('/incidentes-bus/bus/:busId',          incidenteBusCtrl.getByBus);
router.post('/incidentes-bus',                   incidenteBusVal.create, validate, incidenteBusCtrl.create);
router.put('/incidentes-bus/:id',                incidenteBusVal.update, validate, incidenteBusCtrl.update);
router.delete('/incidentes-bus/:id',             incidenteBusCtrl.delete);

// ======================== Fotos ========================
router.get('/fotos',                                      fotoCtrl.getAll);
router.get('/fotos/:id',                                  fotoCtrl.getById);
router.get('/fotos/incidente-bus/:incidenteBusId',        fotoCtrl.getByIncidenteBus);
router.post('/fotos',                                     fotoVal.create, validate, fotoCtrl.create);
router.put('/fotos/:id',                                  fotoVal.update, validate, fotoCtrl.update);
router.delete('/fotos/:id',                               fotoCtrl.delete);

// ======================== Mensajes ========================
router.get('/mensajes',        mensajeCtrl.getAll);
router.get('/mensajes/:id',    mensajeCtrl.getById);
router.post('/mensajes',       mensajeVal.create, validate, mensajeCtrl.create);
router.put('/mensajes/:id',    mensajeVal.update, validate, mensajeCtrl.update);
router.delete('/mensajes/:id', mensajeCtrl.delete);

// ======================== Destinatario Persona ========================
router.get('/destinatario-persona',                            destPersonaCtrl.getAll);
router.get('/destinatario-persona/:id',                        destPersonaCtrl.getById);
router.get('/destinatario-persona/persona/:personaId',         destPersonaCtrl.getByPersona);
router.post('/destinatario-persona',                           destPersonaVal.create, validate, destPersonaCtrl.create);
router.put('/destinatario-persona/:id',                        destPersonaVal.update, validate, destPersonaCtrl.update);
router.delete('/destinatario-persona/:id',                     destPersonaCtrl.delete);

// ======================== Destinatario Grupo ========================
router.get('/destinatario-grupo',        destGrupoCtrl.getAll);
router.get('/destinatario-grupo/:id',    destGrupoCtrl.getById);
router.post('/destinatario-grupo',       destGrupoVal.create, validate, destGrupoCtrl.create);
router.put('/destinatario-grupo/:id',    destGrupoVal.update, validate, destGrupoCtrl.update);
router.delete('/destinatario-grupo/:id', destGrupoCtrl.delete);

// ======================== Grupos ========================
router.get('/grupos',        grupoCtrl.getAll);
router.get('/grupos/:id',    grupoCtrl.getById);
router.post('/grupos',       grupoVal.create, validate, grupoCtrl.create);
router.put('/grupos/:id',    grupoVal.update, validate, grupoCtrl.update);
router.delete('/grupos/:id', grupoCtrl.delete);

// ======================== Grupo-Persona ========================
router.get('/grupo-persona',                        grupoPersonaCtrl.getAll);
router.get('/grupo-persona/:id',                    grupoPersonaCtrl.getById);
router.get('/grupo-persona/grupo/:grupoId',         grupoPersonaCtrl.getByGrupo);
router.post('/grupo-persona',                       grupoPersonaVal.create, validate, grupoPersonaCtrl.create);
router.put('/grupo-persona/:id',                    grupoPersonaVal.update, validate, grupoPersonaCtrl.update);
router.delete('/grupo-persona/:id',                 grupoPersonaCtrl.delete);

// ======================== Analytics ========================
const analyticsCtrl = require('../controllers/analyticsController');
router.get('/analytics/ingresos-metodo-pago', analyticsCtrl.getIngresosMetodoPago);
router.get('/analytics/pasajeros-edades', analyticsCtrl.getPasajerosPorEdad);
router.get('/analytics/incidentes-tendencia', analyticsCtrl.getTendenciaIncidentes);

module.exports = router;
