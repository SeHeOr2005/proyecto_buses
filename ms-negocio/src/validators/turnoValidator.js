const { body } = require('express-validator');

module.exports = {
    create: [
        body('fecha').notEmpty().withMessage('La fecha es obligatoria').isISO8601(),
        body('hora_inicio').notEmpty().withMessage('La hora de inicio es obligatoria').isString(),
        body('hora_fin').notEmpty().withMessage('La hora de fin es obligatoria').isString(),
        body('tipo').optional().isIn(['diurno', 'nocturno', 'mixto']),
        body('conductor_id').notEmpty().withMessage('conductor_id es obligatorio').isMongoId(),
    ],
    update: [
        body('fecha').optional().isISO8601(),
        body('hora_inicio').optional().isString(),
        body('hora_fin').optional().isString(),
        body('tipo').optional().isIn(['diurno', 'nocturno', 'mixto']),
        body('conductor_id').optional().isMongoId(),
    ],
};
