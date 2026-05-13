const Turno = require('../models/turno');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Turno.find().populate('conductor_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Turno.findById(req.params.id).populate('conductor_id');
            if (!data) return res.status(404).json({ error: 'Turno no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Turno(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Turno.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Turno no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Turno.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Turno no encontrado' });
            res.json({ message: 'Turno eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
