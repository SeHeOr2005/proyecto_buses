const { body } = require('express-validator');

module.exports = {
    create: [
        body('nombre').notEmpty().withMessage('El nombre es obligatorio').isString(),
        body('apellido').notEmpty().withMessage('El apellido es obligatorio').isString(),
        body('documento').notEmpty().withMessage('El documento es obligatorio').isString(),
        body('email').notEmpty().withMessage('El email es obligatorio').isEmail(),
        body('telefono').optional().isString(),
        body('user_id').optional().isString(),
        body('licencia').notEmpty().withMessage('La licencia es obligatoria').isString(),
        body('tipo_licencia').notEmpty().withMessage('El tipo de licencia es obligatorio').isString(),
        body('fecha_vencimiento_licencia').notEmpty().withMessage('La fecha de vencimiento es obligatoria').isISO8601(),
        body('estado').optional().isIn(['disponible', 'en_servicio', 'inactivo', 'suspendido']),
    ],
    update: [
        body('nombre').optional().isString(),
        body('apellido').optional().isString(),
        body('documento').optional().isString(),
        body('email').optional().isEmail(),
        body('telefono').optional().isString(),
        body('licencia').optional().isString(),
        body('tipo_licencia').optional().isString(),
        body('fecha_vencimiento_licencia').optional().isISO8601(),
        body('estado').optional().isIn(['disponible', 'en_servicio', 'inactivo', 'suspendido']),
    ],
};
