const mongoose = require('mongoose');
const Persona = require('./persona');

const conductorSchema = new mongoose.Schema({
    licencia:                     { type: String, required: true, unique: true },
    tipo_licencia:                { type: String, required: true },
    fecha_vencimiento_licencia:   { type: Date, required: true },
    estado: { type: String, enum: ['disponible', 'en_servicio', 'inactivo', 'suspendido'], default: 'disponible' },
});

// Discriminador: Conductor hereda todos los campos de Persona
module.exports = Persona.discriminator('Conductor', conductorSchema);
