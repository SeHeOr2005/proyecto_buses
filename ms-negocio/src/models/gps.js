const mongoose = require('mongoose');

const gpsSchema = new mongoose.Schema({
    latitud:              { type: Number, required: true },
    longitud:             { type: Number, required: true },
    ultima_actualizacion: { type: Date, default: Date.now },
    bus_id:               { type: mongoose.Schema.Types.ObjectId, ref: 'Bus', required: true, unique: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('GPS', gpsSchema);
