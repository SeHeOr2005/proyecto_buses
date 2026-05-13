const { body } = require('express-validator');

module.exports = {
    create: [
        body('mensaje_id').notEmpty().withMessage('mensaje_id es obligatorio').isMongoId(),
        body('persona_id').notEmpty().withMessage('persona_id es obligatorio').isMongoId(),
        body('leido').optional().isBoolean(),
    ],
    update: [
        body('mensaje_id').optional().isMongoId(),
        body('persona_id').optional().isMongoId(),
        body('leido').optional().isBoolean(),
    ],
};
