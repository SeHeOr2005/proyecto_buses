const Bus = require('../models/bus');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Bus.find().populate('empresa_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Bus.findById(req.params.id).populate('empresa_id');
            if (!data) return res.status(404).json({ error: 'Bus no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getByEmpresa(req, res) {
        try {
            const data = await Bus.find({ empresa_id: req.params.empresaId }).populate('empresa_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Bus(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Bus.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Bus no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Bus.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Bus no encontrado' });
            res.json({ message: 'Bus eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
