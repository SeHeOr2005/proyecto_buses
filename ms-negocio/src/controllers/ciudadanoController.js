const Ciudadano = require('../models/ciudadano');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Ciudadano.find();
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Ciudadano.findById(req.params.id);
            if (!data) return res.status(404).json({ error: 'Ciudadano no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Ciudadano(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Ciudadano.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Ciudadano no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Ciudadano.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Ciudadano no encontrado' });
            res.json({ message: 'Ciudadano eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
