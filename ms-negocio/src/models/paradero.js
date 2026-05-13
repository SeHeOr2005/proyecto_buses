const mongoose = require('mongoose');

const paraderoSchema = new mongoose.Schema({
    nombre:    { type: String, required: true },
    latitud:   { type: Number, required: true },
    longitud:  { type: Number, required: true },
    direccion: { type: String },
    ruta_id:   { type: mongoose.Schema.Types.ObjectId, ref: 'Ruta', required: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Paradero', paraderoSchema);
