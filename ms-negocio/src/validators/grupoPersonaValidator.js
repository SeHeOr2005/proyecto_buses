const { body } = require('express-validator');

module.exports = {
    create: [
        body('grupo_id').notEmpty().withMessage('grupo_id es obligatorio').isMongoId(),
        body('persona_id').notEmpty().withMessage('persona_id es obligatorio').isMongoId(),
        body('rol').optional().isIn(['miembro', 'administrador']),
    ],
    update: [
        body('grupo_id').optional().isMongoId(),
        body('persona_id').optional().isMongoId(),
        body('rol').optional().isIn(['miembro', 'administrador']),
    ],
};
