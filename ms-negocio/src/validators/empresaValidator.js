const { body } = require('express-validator');

module.exports = {
    create: [
        body('nombre').notEmpty().withMessage('El nombre es obligatorio').isString(),
        body('nit').notEmpty().withMessage('El NIT es obligatorio').isString(),
        body('direccion').notEmpty().withMessage('La dirección es obligatoria').isString(),
        body('telefono').notEmpty().withMessage('El teléfono es obligatorio').isString(),
        body('email').notEmpty().withMessage('El email es obligatorio').isEmail().withMessage('Email inválido'),
        body('estado').optional().isIn(['activo', 'inactivo']),
    ],
    update: [
        body('nombre').optional().isString(),
        body('nit').optional().isString(),
        body('direccion').optional().isString(),
        body('telefono').optional().isString(),
        body('email').optional().isEmail().withMessage('Email inválido'),
        body('estado').optional().isIn(['activo', 'inactivo']),
    ],
};
