const mongoose = require('mongoose');

const incidenteBusSchema = new mongoose.Schema({
    incidente_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Incidente', required: true },
    bus_id:       { type: mongoose.Schema.Types.ObjectId, ref: 'Bus', required: true },
    ubicacion:    { type: String },
    severidad:    { type: String, enum: ['baja', 'media', 'alta', 'critica'], default: 'media' },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('IncidenteBus', incidenteBusSchema);
