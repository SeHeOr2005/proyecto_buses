const Grupo = require('../models/grupo');
const GrupoPersona = require('../models/grupoPersona');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Grupo.find();
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Grupo.findById(req.params.id);
            if (!data) return res.status(404).json({ error: 'Grupo no encontrado' });

            // Incluir miembros del grupo
            const miembros = await GrupoPersona.find({ grupo_id: data._id }).populate('persona_id');
            res.json({ ...data.toObject(), miembros });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Grupo(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Grupo.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Grupo no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Grupo.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Grupo no encontrado' });
            // Eliminar miembros asociados
            await GrupoPersona.deleteMany({ grupo_id: req.params.id });
            res.json({ message: 'Grupo y miembros eliminados' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
