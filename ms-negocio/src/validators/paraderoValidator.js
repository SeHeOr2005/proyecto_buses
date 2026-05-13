const { body } = require('express-validator');

module.exports = {
    create: [
        body('nombre').notEmpty().withMessage('El nombre es obligatorio').isString(),
        body('latitud').notEmpty().withMessage('La latitud es obligatoria').isFloat(),
        body('longitud').notEmpty().withMessage('La longitud es obligatoria').isFloat(),
        body('direccion').optional().isString(),
        body('ruta_id').notEmpty().withMessage('ruta_id es obligatorio').isMongoId(),
    ],
    update: [
        body('nombre').optional().isString(),
        body('latitud').optional().isFloat(),
        body('longitud').optional().isFloat(),
        body('direccion').optional().isString(),
        body('ruta_id').optional().isMongoId(),
    ],
};
