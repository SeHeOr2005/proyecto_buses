const mongoose = require('mongoose');

const nodoSchema = new mongoose.Schema({
    nombre:   { type: String, required: true },
    latitud:  { type: Number, required: true },
    longitud: { type: Number, required: true },
    orden:    { type: Number, required: true },
    ruta_id:  { type: mongoose.Schema.Types.ObjectId, ref: 'Ruta', required: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Nodo', nodoSchema);
