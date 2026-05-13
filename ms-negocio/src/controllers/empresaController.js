const Empresa = require('../models/empresa');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Empresa.find();
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Empresa.findById(req.params.id);
            if (!data) return res.status(404).json({ error: 'Empresa no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Empresa(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Empresa.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Empresa no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Empresa.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Empresa no encontrada' });
            res.json({ message: 'Empresa eliminada' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
