const Direccion = require('../models/direccion');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Direccion.find().populate('ciudadano_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Direccion.findById(req.params.id).populate('ciudadano_id');
            if (!data) return res.status(404).json({ error: 'Dirección no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getByCiudadano(req, res) {
        try {
            const data = await Direccion.findOne({ ciudadano_id: req.params.ciudadanoId }).populate('ciudadano_id');
            if (!data) return res.status(404).json({ error: 'Dirección no encontrada para este ciudadano' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Direccion(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Direccion.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Dirección no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Direccion.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Dirección no encontrada' });
            res.json({ message: 'Dirección eliminada' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
