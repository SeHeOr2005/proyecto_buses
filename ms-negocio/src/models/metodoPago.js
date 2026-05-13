const mongoose = require('mongoose');

const metodoPagoSchema = new mongoose.Schema({
    nombre: { type: String, required: true, unique: true },
    tipo:   { type: String, required: true },
    activo: { type: Boolean, default: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('MetodoPago', metodoPagoSchema);
