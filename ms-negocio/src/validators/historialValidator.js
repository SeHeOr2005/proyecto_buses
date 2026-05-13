const { body } = require('express-validator');

module.exports = {
    create: [
        body('accion').notEmpty().withMessage('La acción es obligatoria').isString(),
        body('detalle').optional().isString(),
        body('boleto_id').notEmpty().withMessage('boleto_id es obligatorio').isMongoId(),
    ],
    update: [
        body('accion').optional().isString(),
        body('detalle').optional().isString(),
        body('boleto_id').optional().isMongoId(),
    ],
};
