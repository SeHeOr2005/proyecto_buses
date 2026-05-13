const mongoose = require('mongoose');

const rutaSchema = new mongoose.Schema({
    nombre:             { type: String, required: true },
    descripcion:        { type: String },
    distancia_km:       { type: Number },
    duracion_estimada:  { type: Number }, // en minutos
    estado:             { type: String, enum: ['activa', 'inactiva', 'en_revision'], default: 'activa' },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Ruta', rutaSchema);
