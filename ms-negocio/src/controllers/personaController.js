const Persona = require('../models/persona');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Persona.find();
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Persona.findById(req.params.id);
            if (!data) return res.status(404).json({ error: 'Persona no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Persona(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Persona.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Persona no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Persona.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Persona no encontrada' });
            res.json({ message: 'Persona eliminada' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
