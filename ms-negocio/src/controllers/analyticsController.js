const Boleto = require('../models/boleto');
const Ciudadano = require('../models/ciudadano');
const Incidente = require('../models/incidente');

module.exports = {
    // HU-014: Ingresos por método de pago
    async getIngresosMetodoPago(req, res) {
        try {
            // Rango de tiempo (ej. últimos 6 meses)
            const mesesAtras = parseInt(req.query.meses) || 6;
            const fechaInicio = new Date();
            fechaInicio.setMonth(fechaInicio.getMonth() - mesesAtras);

            const resultados = await Boleto.aggregate([
                { $match: { fecha_compra: { $gte: fechaInicio }, estado: { $in: ['usado', 'activo', 'vencido'] } } },
                {
                    $lookup: {
                        from: 'metodopagociudadanos', // Asumiendo que guardamos la ref, pero espera... 
                        // En Boleto no guardamos qué método se usó directamente, a menos que lo agreguemos.
                        // REVISIÓN DE MODELO: El boleto en HU-003 requiere saber el método de pago usado.
                        // Vamos a tener que ajustar el modelo Boleto para incluir 'metodo_pago_id'
                        localField: 'metodo_pago_id',
                        foreignField: '_id',
                        as: 'metodo'
                    }
                },
                { $unwind: { path: '$metodo', preserveNullAndEmptyArrays: true } },
                {
                    $group: {
                        _id: {
                            metodo: '$metodo.nombre',
                            mes: { $month: '$fecha_compra' },
                            anio: { $year: '$fecha_compra' }
                        },
                        totalIngresos: { $sum: '$precio' }
                    }
                },
                { $sort: { '_id.anio': 1, '_id.mes': 1 } }
            ]);
            res.json(resultados);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    // HU-015: Distribución porcentual de pasajeros por rango etario
    async getPasajerosPorEdad(req, res) {
        try {
            const ciudadanos = await Ciudadano.find({ fecha_nacimiento: { $exists: true } });
            
            let rangos = {
                'Menores (0-17)': 0,
                'Jóvenes (18-25)': 0,
                'Adultos jóvenes (26-40)': 0,
                'Adultos (41-60)': 0,
                'Adultos mayores (60+)': 0,
                'Sin información': 0
            };

            const hoy = new Date();
            ciudadanos.forEach(c => {
                if (!c.fecha_nacimiento) {
                    rangos['Sin información']++;
                    return;
                }
                const fn = new Date(c.fecha_nacimiento);
                let edad = hoy.getFullYear() - fn.getFullYear();
                const m = hoy.getMonth() - fn.getMonth();
                if (m < 0 || (m === 0 && hoy.getDate() < fn.getDate())) {
                    edad--;
                }

                if (edad <= 17) rangos['Menores (0-17)']++;
                else if (edad <= 25) rangos['Jóvenes (18-25)']++;
                else if (edad <= 40) rangos['Adultos jóvenes (26-40)']++;
                else if (edad <= 60) rangos['Adultos (41-60)']++;
                else rangos['Adultos mayores (60+)']++;
            });

            res.json(rangos);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    // HU-016: Tendencia de incidentes por mes y tipo
    async getTendenciaIncidentes(req, res) {
        try {
            const mesesAtras = parseInt(req.query.meses) || 12;
            const fechaInicio = new Date();
            fechaInicio.setMonth(fechaInicio.getMonth() - mesesAtras);

            const resultados = await Incidente.aggregate([
                { $match: { fecha: { $gte: fechaInicio } } },
                {
                    $group: {
                        _id: {
                            tipo: '$tipo',
                            mes: { $month: '$fecha' },
                            anio: { $year: '$fecha' }
                        },
                        cantidad: { $sum: 1 }
                    }
                },
                { $sort: { '_id.anio': 1, '_id.mes': 1 } }
            ]);
            res.json(resultados);
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    }
};
