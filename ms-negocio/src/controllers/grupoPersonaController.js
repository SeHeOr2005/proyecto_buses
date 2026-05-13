const GrupoPersona = require('../models/grupoPersona');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await GrupoPersona.find().populate('grupo_id').populate('persona_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await GrupoPersona.findById(req.params.id).populate('grupo_id').populate('persona_id');
            if (!data) return res.status(404).json({ error: 'Registro no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getByGrupo(req, res) {
        try {
            const data = await GrupoPersona.find({ grupo_id: req.params.grupoId }).populate('persona_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new GrupoPersona(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await GrupoPersona.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Registro no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await GrupoPersona.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Registro no encontrado' });
            res.json({ message: 'Miembro eliminado del grupo' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
