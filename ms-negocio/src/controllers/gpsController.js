const GPS = require('../models/gps');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await GPS.find().populate('bus_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await GPS.findById(req.params.id).populate('bus_id');
            if (!data) return res.status(404).json({ error: 'GPS no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getByBus(req, res) {
        try {
            const data = await GPS.findOne({ bus_id: req.params.busId }).populate('bus_id');
            if (!data) return res.status(404).json({ error: 'GPS no encontrado para este bus' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new GPS(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await GPS.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'GPS no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await GPS.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'GPS no encontrado' });
            res.json({ message: 'GPS eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
