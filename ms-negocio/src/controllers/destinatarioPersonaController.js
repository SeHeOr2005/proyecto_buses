const DestinatarioPersona = require('../models/destinatarioPersona');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await DestinatarioPersona.find().populate('mensaje_id').populate('persona_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await DestinatarioPersona.findById(req.params.id).populate('mensaje_id').populate('persona_id');
            if (!data) return res.status(404).json({ error: 'Destinatario no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getByPersona(req, res) {
        try {
            const data = await DestinatarioPersona.find({ persona_id: req.params.personaId }).populate('mensaje_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new DestinatarioPersona(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await DestinatarioPersona.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Destinatario no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await DestinatarioPersona.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Destinatario no encontrado' });
            res.json({ message: 'Destinatario eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
