const mongoose = require('mongoose');

const programacionSchema = new mongoose.Schema({
    fecha:       { type: Date, required: true },
    hora_inicio: { type: String, required: true },
    hora_fin:    { type: String, required: true },
    estado:      { type: String, enum: ['programada', 'en_curso', 'finalizada', 'cancelada'], default: 'programada' },
    ruta_id:     { type: mongoose.Schema.Types.ObjectId, ref: 'Ruta', required: true },
    bus_id:      { type: mongoose.Schema.Types.ObjectId, ref: 'Bus', required: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Programacion', programacionSchema);
