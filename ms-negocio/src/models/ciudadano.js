const mongoose = require('mongoose');
const Persona = require('./persona');

const ciudadanoSchema = new mongoose.Schema({
    fecha_nacimiento: { type: Date },
});

// Discriminador: Ciudadano hereda todos los campos de Persona
module.exports = Persona.discriminator('Ciudadano', ciudadanoSchema);
