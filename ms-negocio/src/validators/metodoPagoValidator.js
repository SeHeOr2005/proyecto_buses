const { body } = require('express-validator');

module.exports = {
    create: [
        body('nombre').notEmpty().withMessage('El nombre es obligatorio').isString(),
        body('tipo').notEmpty().withMessage('El tipo es obligatorio').isString(),
        body('activo').optional().isBoolean(),
    ],
    update: [
        body('nombre').optional().isString(),
        body('tipo').optional().isString(),
        body('activo').optional().isBoolean(),
    ],
};
