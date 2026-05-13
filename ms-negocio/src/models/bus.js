const mongoose = require('mongoose');

const busSchema = new mongoose.Schema({
    placa:      { type: String, required: true, unique: true },
    modelo:     { type: String, required: true },
    capacidad:  { type: Number, required: true },
    estado:     { type: String, enum: ['activo', 'inactivo', 'mantenimiento'], default: 'activo' },
    empresa_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Empresa', required: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Bus', busSchema);
