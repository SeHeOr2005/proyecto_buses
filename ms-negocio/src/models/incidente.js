const mongoose = require('mongoose');

const incidenteSchema = new mongoose.Schema({
    tipo:          { type: String, required: true },
    descripcion:   { type: String, required: true },
    fecha:         { type: Date, default: Date.now },
    estado:        { type: String, enum: ['reportado', 'en_revision', 'resuelto', 'descartado'], default: 'reportado' },
    reportado_por: { type: mongoose.Schema.Types.ObjectId, ref: 'Persona' },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Incidente', incidenteSchema);
