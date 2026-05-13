const mongoose = require('mongoose');

const boletoSchema = new mongoose.Schema({
    fecha_compra:     { type: Date, default: Date.now },
    precio:           { type: Number, required: true },
    estado:           { type: String, enum: ['activo', 'usado', 'cancelado', 'vencido'], default: 'activo' },
    ciudadano_id:     { type: mongoose.Schema.Types.ObjectId, ref: 'Ciudadano', required: true },
    programacion_id:  { type: mongoose.Schema.Types.ObjectId, ref: 'Programacion', required: true },
    ruta_id:          { type: mongoose.Schema.Types.ObjectId, ref: 'Ruta', required: true },
    metodo_pago_id:   { type: mongoose.Schema.Types.ObjectId, ref: 'MetodoPago' },
}, { timestamps: true, versionKey: false });

module.exports = mongoose.model('Boleto', boletoSchema);
