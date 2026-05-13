require('dotenv').config();

const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const connectDB = require('./config/database');
const routes = require('./routes');

const app = express();
const PORT = process.env.PORT || 3200;

// Middlewares
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

// Rutas
app.use('/api', routes);

// Health check
app.get('/health', (req, res) => {
    res.json({ status: 'OK', service: 'ms-negocio', port: PORT });
});

// Conexión a MongoDB e inicio del servidor
connectDB().then(() => {
    app.listen(PORT, () => {
        console.log(`🚌 ms-negocio corriendo en http://localhost:${PORT}`);
    });
});
