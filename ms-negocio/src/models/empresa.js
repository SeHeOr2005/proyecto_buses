const mongoose = require('mongoose');

const empresaSchema = new mongoose.Schema({
    nombre:    { type: String, required: true },
    nit:       { type: String, required: true, unique: true },
    direccion: { type: String, required: true },
    telefono:  { type: String, required: true },
    email:     { type: String, required: true },
    estado:    { type: String, enum: ['activo', 'inactivo'], default: 'activo' },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Empresa', empresaSchema);
