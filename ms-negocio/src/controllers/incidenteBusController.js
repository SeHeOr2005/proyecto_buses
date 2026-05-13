const IncidenteBus = require('../models/incidenteBus');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await IncidenteBus.find().populate('incidente_id').populate('bus_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await IncidenteBus.findById(req.params.id).populate('incidente_id').populate('bus_id');
            if (!data) return res.status(404).json({ error: 'IncidenteBus no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getByBus(req, res) {
        try {
            const data = await IncidenteBus.find({ bus_id: req.params.busId }).populate('incidente_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new IncidenteBus(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await IncidenteBus.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'IncidenteBus no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await IncidenteBus.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'IncidenteBus no encontrado' });
            res.json({ message: 'IncidenteBus eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
