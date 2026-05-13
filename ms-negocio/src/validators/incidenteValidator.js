const { body } = require('express-validator');

module.exports = {
    create: [
        body('tipo').notEmpty().withMessage('El tipo es obligatorio').isString(),
        body('descripcion').notEmpty().withMessage('La descripción es obligatoria').isString(),
        body('estado').optional().isIn(['reportado', 'en_revision', 'resuelto', 'descartado']),
        body('reportado_por').optional().isMongoId(),
    ],
    update: [
        body('tipo').optional().isString(),
        body('descripcion').optional().isString(),
        body('estado').optional().isIn(['reportado', 'en_revision', 'resuelto', 'descartado']),
        body('reportado_por').optional().isMongoId(),
    ],
};
