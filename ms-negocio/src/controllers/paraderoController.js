const Paradero = require('../models/paradero');

module.exports = {
    async getAll(req, res) {
        try {
            const data = await Paradero.find().populate('ruta_id');
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getById(req, res) {
        try {
            const data = await Paradero.findById(req.params.id).populate('ruta_id');
            if (!data) return res.status(404).json({ error: 'Paradero no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async getCercanos(req, res) {
        try {
            const lat = parseFloat(req.query.lat);
            const lng = parseFloat(req.query.lng);
            if (isNaN(lat) || isNaN(lng)) {
                return res.status(400).json({ error: 'lat y lng son requeridos' });
            }

            // Para simplificar, obtenemos todos y calculamos la distancia.
            // Para producción con miles de paraderos, se debería usar $geoNear de MongoDB.
            const paraderos = await Paradero.find().populate('ruta_id');
            
            const conDistancia = paraderos.map(p => {
                // Haversine formula simple
                const R = 6371e3; // metres
                const φ1 = lat * Math.PI/180;
                const φ2 = p.latitud * Math.PI/180;
                const Δφ = (p.latitud-lat) * Math.PI/180;
                const Δλ = (p.longitud-lng) * Math.PI/180;

                const a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                          Math.cos(φ1) * Math.cos(φ2) *
                          Math.sin(Δλ/2) * Math.sin(Δλ/2);
                const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

                const d = R * c; // in metres
                return { ...p.toObject(), distancia: Math.round(d) };
            });

            // Ordenar por distancia y tomar los 5 más cercanos
            conDistancia.sort((a, b) => a.distancia - b.distancia);
            res.json(conDistancia.slice(0, 5));
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async create(req, res) {
        try {
            const data = new Paradero(req.body);
            await data.save();
            res.status(201).json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async update(req, res) {
        try {
            const data = await Paradero.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
            if (!data) return res.status(404).json({ error: 'Paradero no encontrado' });
            res.json(data);
        } catch (error) {
            res.status(400).json({ error: error.message });
        }
    },

    async delete(req, res) {
        try {
            const data = await Paradero.findByIdAndDelete(req.params.id);
            if (!data) return res.status(404).json({ error: 'Paradero no encontrado' });
            res.json({ message: 'Paradero eliminado' });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
};
