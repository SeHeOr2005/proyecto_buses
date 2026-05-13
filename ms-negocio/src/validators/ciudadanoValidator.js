const { body } = require('express-validator');

module.exports = {
    create: [
        body('nombre').notEmpty().withMessage('El nombre es obligatorio').isString(),
        body('apellido').notEmpty().withMessage('El apellido es obligatorio').isString(),
        body('documento').notEmpty().withMessage('El documento es obligatorio').isString(),
        body('email').notEmpty().withMessage('El email es obligatorio').isEmail(),
        body('telefono').optional().isString(),
        body('user_id').optional().isString(),
        body('fecha_nacimiento').optional().isISO8601(),
    ],
    update: [
        body('nombre').optional().isString(),
        body('apellido').optional().isString(),
        body('documento').optional().isString(),
        body('email').optional().isEmail(),
        body('telefono').optional().isString(),
        body('fecha_nacimiento').optional().isISO8601(),
    ],
};
