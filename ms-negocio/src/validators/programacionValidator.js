const { body } = require('express-validator');

module.exports = {
    create: [
        body('fecha').notEmpty().withMessage('La fecha es obligatoria').isISO8601(),
        body('hora_inicio').notEmpty().withMessage('La hora de inicio es obligatoria').isString(),
        body('hora_fin').notEmpty().withMessage('La hora de fin es obligatoria').isString(),
        body('estado').optional().isIn(['programada', 'en_curso', 'finalizada', 'cancelada']),
        body('ruta_id').notEmpty().withMessage('ruta_id es obligatorio').isMongoId(),
        body('bus_id').notEmpty().withMessage('bus_id es obligatorio').isMongoId(),
    ],
    update: [
        body('fecha').optional().isISO8601(),
        body('hora_inicio').optional().isString(),
        body('hora_fin').optional().isString(),
        body('estado').optional().isIn(['programada', 'en_curso', 'finalizada', 'cancelada']),
        body('ruta_id').optional().isMongoId(),
        body('bus_id').optional().isMongoId(),
    ],
};
