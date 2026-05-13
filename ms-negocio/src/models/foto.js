const mongoose = require('mongoose');

const fotoSchema = new mongoose.Schema({
    url:              { type: String, required: true },
    descripcion:      { type: String },
    incidente_bus_id: { type: mongoose.Schema.Types.ObjectId, ref: 'IncidenteBus', required: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Foto', fotoSchema);
