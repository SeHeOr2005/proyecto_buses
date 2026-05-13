const { body } = require('express-validator');

module.exports = {
    create: [
        body('emisor_id').notEmpty().withMessage('emisor_id es obligatorio').isMongoId(),
        body('contenido').notEmpty().withMessage('El contenido es obligatorio').isString(),
    ],
    update: [
        body('emisor_id').optional().isMongoId(),
        body('contenido').optional().isString(),
    ],
};
