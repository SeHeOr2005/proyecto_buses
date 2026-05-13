const mongoose = require('mongoose');

const destinatarioPersonaSchema = new mongoose.Schema({
    mensaje_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Mensaje', required: true },
    persona_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Persona', required: true },
    leido:      { type: Boolean, default: false },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('DestinatarioPersona', destinatarioPersonaSchema);
