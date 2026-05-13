const MetodoPago = require('../models/metodoPago');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await MetodoPago.find();
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await MetodoPago.findById(req.params.id);
            if (!data) return res.status(404).json({ error: 'Método de pago no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new MetodoPago(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await MetodoPago.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Método de pago no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await MetodoPago.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Método de pago no encontrado' });
            res.json({ message: 'Método de pago eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
