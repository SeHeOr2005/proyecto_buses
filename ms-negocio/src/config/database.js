const mongoose = require('mongoose');

const connectDB = async () => {
    try {
        const uri = process.env.MONGODB_URI;
        await mongoose.connect(uri);
        console.log('✅ MongoDB conectado — Base de datos: db_negocio');
    } catch (error) {
        console.error('❌ Error al conectar MongoDB:', error.message);
        process.exit(1);
    }
};

module.exports = connectDB;
