const mongoose = require('mongoose');

const historialSchema = new mongoose.Schema({
    accion:    { type: String, required: true },
    fecha:     { type: Date, default: Date.now },
    detalle:   { type: String },
    boleto_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Boleto', required: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Historial', historialSchema);
