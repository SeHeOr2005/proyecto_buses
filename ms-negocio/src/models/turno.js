const mongoose = require('mongoose');

const turnoSchema = new mongoose.Schema({
    fecha:        { type: Date, required: true },
    hora_inicio:  { type: String, required: true },
    hora_fin:     { type: String, required: true },
    tipo:         { type: String, enum: ['diurno', 'nocturno', 'mixto'], default: 'diurno' },
    conductor_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Conductor', required: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Turno', turnoSchema);
