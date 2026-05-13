const Mensaje = require('../models/mensaje');
const DestinatarioPersona = require('../models/destinatarioPersona');
const DestinatarioGrupo = require('../models/destinatarioGrupo');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Mensaje.find().populate('emisor_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Mensaje.findById(req.params.id).populate('emisor_id');
            if (!data) return res.status(404).json({ error: 'Mensaje no encontrado' });

            // Obtener destinatarios
            const destinatariosPersona = await DestinatarioPersona.find({ mensaje_id: data._id }).populate('persona_id');
            const destinatariosGrupo = await DestinatarioGrupo.find({ mensaje_id: data._id }).populate('grupo_id');

            res.json({
                ...data.toObject(),
                destinatarios_persona: destinatariosPersona,
                destinatarios_grupo: destinatariosGrupo,
            });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Mensaje(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Mensaje.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Mensaje no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Mensaje.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Mensaje no encontrado' });
            // Eliminar destinatarios asociados
            await DestinatarioPersona.deleteMany({ mensaje_id: req.params.id });
            await DestinatarioGrupo.deleteMany({ mensaje_id: req.params.id });
            res.json({ message: 'Mensaje y destinatarios eliminados' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
