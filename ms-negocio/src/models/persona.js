const mongoose = require('mongoose');

const personaSchema = new mongoose.Schema({
    nombre:    { type: String, required: true },
    apellido:  { type: String, required: true },
    documento: { type: String, required: true, unique: true },
    telefono:  { type: String },
    email:     { type: String, required: true },
    // Referencia cruzada al User de ms-security (diferentes BDs, no se usa ref/populate)
    user_id:   { type: String },
}, {
    timestamps: true,
    versionKey: false,
    discriminatorKey: 'tipo_persona',
});

module.exports = mongoose.model('Persona', personaSchema);
