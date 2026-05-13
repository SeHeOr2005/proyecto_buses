const mongoose = require('mongoose');

const mensajeSchema = new mongoose.Schema({
    emisor_id:   { type: mongoose.Schema.Types.ObjectId, ref: 'Persona', required: true },
    contenido:   { type: String, required: true },
    fecha_envio: { type: Date, default: Date.now },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Mensaje', mensajeSchema);
