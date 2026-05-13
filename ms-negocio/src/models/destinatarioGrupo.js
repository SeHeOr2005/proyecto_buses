const mongoose = require('mongoose');

const destinatarioGrupoSchema = new mongoose.Schema({
    mensaje_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Mensaje', required: true },
    grupo_id:   { type: mongoose.Schema.Types.ObjectId, ref: 'Grupo', required: true },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('DestinatarioGrupo', destinatarioGrupoSchema);
