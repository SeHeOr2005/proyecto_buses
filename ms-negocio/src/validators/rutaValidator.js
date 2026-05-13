const { body } = require('express-validator');

module.exports = {
    create: [
        body('nombre').notEmpty().withMessage('El nombre es obligatorio').isString(),
        body('descripcion').optional().isString(),
        body('distancia_km').optional().isFloat({ min: 0 }),
        body('duracion_estimada').optional().isInt({ min: 0 }),
        body('estado').optional().isIn(['activa', 'inactiva', 'en_revision']),
    ],
    update: [
        body('nombre').optional().isString(),
        body('descripcion').optional().isString(),
        body('distancia_km').optional().isFloat({ min: 0 }),
        body('duracion_estimada').optional().isInt({ min: 0 }),
        body('estado').optional().isIn(['activa', 'inactiva', 'en_revision']),
    ],
};
