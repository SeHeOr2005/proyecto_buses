const mongoose = require('mongoose');

const grupoPersonaSchema = new mongoose.Schema({
    grupo_id:   { type: mongoose.Schema.Types.ObjectId, ref: 'Grupo', required: true },
    persona_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Persona', required: true },
    rol:        { type: String, enum: ['miembro', 'administrador'], default: 'miembro' },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('GrupoPersona', grupoPersonaSchema);
