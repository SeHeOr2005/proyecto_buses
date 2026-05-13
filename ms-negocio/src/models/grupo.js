const mongoose = require('mongoose');

const grupoSchema = new mongoose.Schema({
    nombre:      { type: String, required: true },
    descripcion: { type: String },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Grupo', grupoSchema);
