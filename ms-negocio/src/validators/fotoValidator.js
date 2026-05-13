const { body } = require('express-validator');

module.exports = {
    create: [
        body('url').notEmpty().withMessage('La URL es obligatoria').isString(),
        body('descripcion').optional().isString(),
        body('incidente_bus_id').notEmpty().withMessage('incidente_bus_id es obligatorio').isMongoId(),
    ],
    update: [
        body('url').optional().isString(),
        body('descripcion').optional().isString(),
        body('incidente_bus_id').optional().isMongoId(),
    ],
};
