const mongoose = require('mongoose');

const uri = 'mongodb+srv://juannaranjo56321:Leondelcovid19.@clusternarver.pwnrxr8.mongodb.net/db_security?appName=ClusterNarver';

async function run() {
  await mongoose.connect(uri);
  const db = mongoose.connection.db;

  const usersCollection = db.collection('user');
  
  // Create user if not exists
  const email = 'juannaranjo56321@gmail.com';
  let user = await usersCollection.findOne({ email: email });
  
  if (!user) {
    console.log("Usuario no encontrado, creando uno falso para forzar su existencia y asignarle el rol...");
    const res = await usersCollection.insertOne({
      name: "Juan Naranjo",
      email: email,
      active: true,
      authProvider: "google.com",
      _class: "com.sho.ms_security.models.User"
    });
    user = await usersCollection.findOne({ _id: res.insertedId });
  } else {
    console.log("Usuario encontrado:", user.email);
  }

  const rolesCollection = db.collection('role');
  const adminRole = await rolesCollection.findOne({ name: 'ADMINISTRADOR_SISTEMA' });

  if (user && adminRole) {
    const userRoleColl = db.collection('userRole');
    
    // Asignar rol
    await userRoleColl.updateOne(
      { "user.$id": user._id },
      { 
        $set: { 
          "user": { "$ref": "user", "$id": user._id },
          "role": { "$ref": "role", "$id": adminRole._id },
          "_class": "com.sho.ms_security.models.UserRole"
        }
      },
      { upsert: true }
    );
    console.log(`¡${email} ahora tiene el rol ADMINISTRADOR_SISTEMA!`);
  } else {
     console.log("No se pudo asignar el rol.");
  }

  mongoose.disconnect();
}
run().catch(console.error);
