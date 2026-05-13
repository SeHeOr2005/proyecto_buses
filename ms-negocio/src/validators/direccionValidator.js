const { body } = require('express-validator');

module.exports = {
    create: [
        body('calle').notEmpty().withMessage('La calle es obligatoria').isString(),
        body('ciudad').notEmpty().withMessage('La ciudad es obligatoria').isString(),
        body('departamento').notEmpty().withMessage('El departamento es obligatorio').isString(),
        body('codigo_postal').optional().isString(),
        body('ciudadano_id').notEmpty().withMessage('ciudadano_id es obligatorio').isMongoId(),
    ],
    update: [
        body('calle').optional().isString(),
        body('ciudad').optional().isString(),
        body('departamento').optional().isString(),
        body('codigo_postal').optional().isString(),
        body('ciudadano_id').optional().isMongoId(),
    ],
};
