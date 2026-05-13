const { body } = require('express-validator');

module.exports = {
    create: [
        body('nombre').notEmpty().withMessage('El nombre es obligatorio').isString(),
        body('descripcion').optional().isString(),
    ],
    update: [
        body('nombre').optional().isString(),
        body('descripcion').optional().isString(),
    ],
};
