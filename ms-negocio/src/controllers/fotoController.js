const Foto = require('../models/foto');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Foto.find().populate('incidente_bus_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Foto.findById(req.params.id).populate('incidente_bus_id');
            if (!data) return res.status(404).json({ error: 'Foto no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getByIncidenteBus(req, res) {
        try {
            const data = await Foto.find({ incidente_bus_id: req.params.incidenteBusId });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Foto(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Foto.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Foto no encontrada' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Foto.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Foto no encontrada' });
            res.json({ message: 'Foto eliminada' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
