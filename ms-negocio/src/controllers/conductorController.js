const Conductor = require('../models/conductor');
const Turno = require('../models/turno');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Conductor.find();
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Conductor.findById(req.params.id);
            if (!data) return res.status(404).json({ error: 'Conductor no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getTurnos(req, res) {
        try {
            const turnos = await Turno.find({ conductor_id: req.params.id }).populate('conductor_id');
            res.json(turnos);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Conductor(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Conductor.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Conductor no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Conductor.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Conductor no encontrado' });
            res.json({ message: 'Conductor eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
