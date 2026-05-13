const mongoose = require('mongoose');

const metodoPagoCiudadanoSchema = new mongoose.Schema({
    ciudadano_id:   { type: mongoose.Schema.Types.ObjectId, ref: 'Ciudadano', required: true },
    metodo_pago_id: { type: mongoose.Schema.Types.ObjectId, ref: 'MetodoPago', required: true },
    detalle:        { type: String },
    predeterminado: { type: Boolean, default: false },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('MetodoPagoCiudadano', metodoPagoCiudadanoSchema);
