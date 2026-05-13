const Incidente = require('../models/incidente');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Incidente.find().populate('reportado_por');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Incidente.findById(req.params.id).populate('reportado_por');
            if (!data) return res.status(404).json({ error: 'Incidente no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Incidente(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Incidente.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Incidente no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Incidente.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Incidente no encontrado' });
            res.json({ message: 'Incidente eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
