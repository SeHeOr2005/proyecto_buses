const Nodo = require('../models/nodo');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Nodo.find().populate('ruta_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Nodo.findById(req.params.id).populate('ruta_id');
            if (!data) return res.status(404).json({ error: 'Nodo no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Nodo(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Nodo.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Nodo no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Nodo.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Nodo no encontrado' });
            res.json({ message: 'Nodo eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
