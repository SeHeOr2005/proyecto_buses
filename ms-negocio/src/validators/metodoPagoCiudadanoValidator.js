const { body } = require('express-validator');

module.exports = {
    create: [
        body('ciudadano_id').notEmpty().withMessage('ciudadano_id es obligatorio').isMongoId(),
        body('metodo_pago_id').notEmpty().withMessage('metodo_pago_id es obligatorio').isMongoId(),
        body('detalle').optional().isString(),
        body('predeterminado').optional().isBoolean(),
    ],
    update: [
        body('ciudadano_id').optional().isMongoId(),
        body('metodo_pago_id').optional().isMongoId(),
        body('detalle').optional().isString(),
        body('predeterminado').optional().isBoolean(),
    ],
};
