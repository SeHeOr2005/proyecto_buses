const { body } = require('express-validator');

module.exports = {
    create: [
        body('precio').notEmpty().withMessage('El precio es obligatorio').isFloat({ min: 0 }),
        body('estado').optional().isIn(['activo', 'usado', 'cancelado', 'vencido']),
        body('ciudadano_id').notEmpty().withMessage('ciudadano_id es obligatorio').isMongoId(),
        body('programacion_id').notEmpty().withMessage('programacion_id es obligatorio').isMongoId(),
        body('ruta_id').notEmpty().withMessage('ruta_id es obligatorio').isMongoId(),
    ],
    update: [
        body('precio').optional().isFloat({ min: 0 }),
        body('estado').optional().isIn(['activo', 'usado', 'cancelado', 'vencido']),
        body('ciudadano_id').optional().isMongoId(),
        body('programacion_id').optional().isMongoId(),
        body('ruta_id').optional().isMongoId(),
    ],
};
