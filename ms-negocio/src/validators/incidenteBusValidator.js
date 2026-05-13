const { body } = require('express-validator');

module.exports = {
    create: [
        body('incidente_id').notEmpty().withMessage('incidente_id es obligatorio').isMongoId(),
        body('bus_id').notEmpty().withMessage('bus_id es obligatorio').isMongoId(),
        body('ubicacion').optional().isString(),
        body('severidad').optional().isIn(['baja', 'media', 'alta', 'critica']),
    ],
    update: [
        body('incidente_id').optional().isMongoId(),
        body('bus_id').optional().isMongoId(),
        body('ubicacion').optional().isString(),
        body('severidad').optional().isIn(['baja', 'media', 'alta', 'critica']),
    ],
};
