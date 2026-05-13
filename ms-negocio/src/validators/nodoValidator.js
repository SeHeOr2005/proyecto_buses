const { body } = require('express-validator');

module.exports = {
    create: [
        body('nombre').notEmpty().withMessage('El nombre es obligatorio').isString(),
        body('latitud').notEmpty().withMessage('La latitud es obligatoria').isFloat(),
        body('longitud').notEmpty().withMessage('La longitud es obligatoria').isFloat(),
        body('orden').notEmpty().withMessage('El orden es obligatorio').isInt({ min: 0 }),
        body('ruta_id').notEmpty().withMessage('ruta_id es obligatorio').isMongoId(),
    ],
    update: [
        body('nombre').optional().isString(),
        body('latitud').optional().isFloat(),
        body('longitud').optional().isFloat(),
        body('orden').optional().isInt({ min: 0 }),
        body('ruta_id').optional().isMongoId(),
    ],
};
