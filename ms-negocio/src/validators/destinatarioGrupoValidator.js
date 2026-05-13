const { body } = require('express-validator');

module.exports = {
    create: [
        body('mensaje_id').notEmpty().withMessage('mensaje_id es obligatorio').isMongoId(),
        body('grupo_id').notEmpty().withMessage('grupo_id es obligatorio').isMongoId(),
    ],
    update: [
        body('mensaje_id').optional().isMongoId(),
        body('grupo_id').optional().isMongoId(),
    ],
};
