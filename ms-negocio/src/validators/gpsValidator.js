const { body } = require('express-validator');

module.exports = {
    create: [
        body('latitud').notEmpty().withMessage('La latitud es obligatoria').isFloat(),
        body('longitud').notEmpty().withMessage('La longitud es obligatoria').isFloat(),
        body('bus_id').notEmpty().withMessage('bus_id es obligatorio').isMongoId(),
    ],
    update: [
        body('latitud').optional().isFloat(),
        body('longitud').optional().isFloat(),
        body('bus_id').optional().isMongoId(),
    ],
};
