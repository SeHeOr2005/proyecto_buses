const { body } = require('express-validator');

module.exports = {
    create: [
        body('nombre').notEmpty().withMessage('El nombre es obligatorio').isString(),
        body('apellido').notEmpty().withMessage('El apellido es obligatorio').isString(),
        body('documento').notEmpty().withMessage('El documento es obligatorio').isString(),
        body('telefono').optional().isString(),
        body('email').notEmpty().withMessage('El email es obligatorio').isEmail(),
        body('user_id').optional().isString(),
    ],
    update: [
        body('nombre').optional().isString(),
        body('apellido').optional().isString(),
        body('documento').optional().isString(),
        body('telefono').optional().isString(),
        body('email').optional().isEmail(),
        body('user_id').optional().isString(),
    ],
};
