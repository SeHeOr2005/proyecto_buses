/**
 * Migración / Seed inicial para ms-negocio
 * Ejecutar: npm run migrate
 */
require('dotenv').config({ path: require('path').resolve(__dirname, '..', '.env') });

const mongoose = require('mongoose');
const MetodoPago = require('../src/models/metodoPago');

const SEED_DATA = {
    metodosPago: [
        { nombre: 'Efectivo',         tipo: 'fisico',      activo: true },
        { nombre: 'Tarjeta Crédito',  tipo: 'electronico', activo: true },
        { nombre: 'Tarjeta Débito',   tipo: 'electronico', activo: true },
        { nombre: 'PSE',              tipo: 'electronico', activo: true },
        { nombre: 'Nequi',            tipo: 'electronico', activo: true },
        { nombre: 'Daviplata',        tipo: 'electronico', activo: true },
    ],
};

async function migrate() {
    try {
        await mongoose.connect(process.env.MONGODB_URI);
        console.log('✅ Conectado a MongoDB para migración');

        // Métodos de pago
        const existingCount = await MetodoPago.countDocuments();
        if (existingCount === 0) {
            await MetodoPago.insertMany(SEED_DATA.metodosPago);
            console.log(`✅ ${SEED_DATA.metodosPago.length} métodos de pago insertados`);
        } else {
            console.log(`⏭️  Métodos de pago ya existen (${existingCount}), saltando seed`);
        }

        console.log('✅ Migración completada');
    } catch (error) {
        console.error('❌ Error en migración:', error.message);
    } finally {
        await mongoose.disconnect();
        console.log('🔌 Desconectado de MongoDB');
    }
}

migrate();
