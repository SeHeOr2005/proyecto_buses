const MetodoPagoCiudadano = require('../models/metodoPagoCiudadano');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await MetodoPagoCiudadano.find().populate('ciudadano_id').populate('metodo_pago_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await MetodoPagoCiudadano.findById(req.params.id).populate('ciudadano_id').populate('metodo_pago_id');
            if (!data) return res.status(404).json({ error: 'Registro no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getByCiudadano(req, res) {
        try {
            const data = await MetodoPagoCiudadano.find({ ciudadano_id: req.params.ciudadanoId }).populate('metodo_pago_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new MetodoPagoCiudadano(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await MetodoPagoCiudadano.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Registro no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await MetodoPagoCiudadano.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Registro no encontrado' });
            res.json({ message: 'Método de pago del ciudadano eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
