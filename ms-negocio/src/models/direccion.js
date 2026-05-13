const mongoose = require('mongoose');

const direccionSchema = new mongoose.Schema({
    calle:         { type: String, required: true },
    ciudad:        { type: String, required: true },
    departamento:  { type: String, required: true },
    codigo_postal: { type: String },
    ciudadano_id:  { type: mongoose.Schema.Types.ObjectId, ref: 'Ciudadano', required: true, unique: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Direccion', direccionSchema);
