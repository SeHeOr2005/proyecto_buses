const mongoose = require('mongoose');

const uri = 'mongodb+srv://juannaranjo56321:Leondelcovid19.@clusternarver.pwnrxr8.mongodb.net/db_security?appName=ClusterNarver';

async function run() {
  await mongoose.connect(uri);
  const db = mongoose.connection.db;

  const users = await db.collection('user').find({}).toArray();
  console.log("Usuarios en db_security:", users.map(u => ({_id: u._id, email: u.email})));

  const roles = await db.collection('role').find({}).toArray();
  const adminRole = roles.find(r => r.name === 'ADMINISTRADOR_SISTEMA');

  if (users.length > 0 && adminRole) {
    const userRoleColl = db.collection('userRole');
    for (let u of users) {
      console.log(`Asignando ADMIN a ${u.email}...`);
      await userRoleColl.updateOne(
        { "user.$id": u._id },
        { 
          $set: { 
            "user": { "$ref": "user", "$id": u._id },
            "role": { "$ref": "role", "$id": adminRole._id }
          }
        },
        { upsert: true }
      );
    }
    console.log("¡Todos los usuarios ahora son administradores!");
  }

  mongoose.disconnect();
}
run().catch(console.error);
