const Historial = require('../models/historial');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Historial.find().populate('boleto_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Historial.findById(req.params.id).populate('boleto_id');
            if (!data) return res.status(404).json({ error: 'Historial no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getByBoleto(req, res) {
        try {
            const data = await Historial.find({ boleto_id: req.params.boletoId });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Historial(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Historial.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Historial no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Historial.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Historial no encontrado' });
            res.json({ message: 'Historial eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
