const Boleto = require('../models/boleto');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Boleto.find().populate('ciudadano_id').populate('programacion_id').populate('ruta_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Boleto.findById(req.params.id).populate('ciudadano_id').populate('programacion_id').populate('ruta_id');
            if (!data) return res.status(404).json({ error: 'Boleto no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getByCiudadano(req, res) {
        try {
            const data = await Boleto.find({ ciudadano_id: req.params.ciudadanoId }).populate('programacion_id').populate('ruta_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Boleto(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Boleto.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Boleto no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Boleto.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Boleto no encontrado' });
            res.json({ message: 'Boleto eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
