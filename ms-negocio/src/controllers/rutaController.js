const Ruta = require('../models/ruta');
const Nodo = require('../models/nodo');
const Paradero = require('../models/paradero');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Ruta.find();
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Ruta.findById(req.params.id);
            if (!data) return res.status(404).json({ error: 'Ruta no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getNodos(req, res) {
        try {
            const nodos = await Nodo.find({ ruta_id: req.params.id }).sort({ orden: 1 });
            res.json(nodos);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getParaderos(req, res) {
        try {
            const paraderos = await Paradero.find({ ruta_id: req.params.id });
            res.json(paraderos);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Ruta(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Ruta.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Ruta no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Ruta.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Ruta no encontrada' });
            res.json({ message: 'Ruta eliminada' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
