const { body } = require('express-validator');

module.exports = {
    create: [
        body('placa').notEmpty().withMessage('La placa es obligatoria').isString(),
        body('modelo').notEmpty().withMessage('El modelo es obligatorio').isString(),
        body('capacidad').notEmpty().withMessage('La capacidad es obligatoria').isInt({ min: 1 }).withMessage('Capacidad debe ser mayor a 0'),
        body('estado').optional().isIn(['activo', 'inactivo', 'mantenimiento']),
        body('empresa_id').notEmpty().withMessage('empresa_id es obligatorio').isMongoId().withMessage('empresa_id inválido'),
    ],
    update: [
        body('placa').optional().isString(),
        body('modelo').optional().isString(),
        body('capacidad').optional().isInt({ min: 1 }),
        body('estado').optional().isIn(['activo', 'inactivo', 'mantenimiento']),
        body('empresa_id').optional().isMongoId(),
    ],
};
