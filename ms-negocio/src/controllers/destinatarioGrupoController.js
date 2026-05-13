const DestinatarioGrupo = require('../models/destinatarioGrupo');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await DestinatarioGrupo.find().populate('mensaje_id').populate('grupo_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await DestinatarioGrupo.findById(req.params.id).populate('mensaje_id').populate('grupo_id');
            if (!data) return res.status(404).json({ error: 'Destinatario de grupo no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new DestinatarioGrupo(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await DestinatarioGrupo.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Destinatario de grupo no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await DestinatarioGrupo.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Destinatario de grupo no encontrado' });
            res.json({ message: 'Destinatario de grupo eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
