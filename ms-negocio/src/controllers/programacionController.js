const Programacion = require('../models/programacion');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Programacion.find().populate('ruta_id').populate('bus_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Programacion.findById(req.params.id).populate('ruta_id').populate('bus_id');
            if (!data) return res.status(404).json({ error: 'Programación no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Programacion(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Programacion.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Programación no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Programacion.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Programación no encontrada' });
            res.json({ message: 'Programación eliminada' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
