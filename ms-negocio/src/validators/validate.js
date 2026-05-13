const { validationResult } = require('express-validator');

/**
 * Middleware que revisa los errores de express-validator.
 * Si hay errores, retorna 400 con el detalle. Si no, continúa.
 */
const validate = (req, res, next) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
    }
    next();
};

module.exports = validate;
