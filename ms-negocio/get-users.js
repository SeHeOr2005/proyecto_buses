const mongoose = require('mongoose');

const uri = 'mongodb+srv://juannaranjo56321:Leondelcovid19.@clusternarver.pwnrxr8.mongodb.net/db_security?appName=ClusterNarver';

async function run() {
  await mongoose.connect(uri);
  const db = mongoose.connection.db;

  const usersCollection = db.collection('users');
  const users = await usersCollection.find({}).toArray();
  console.log("Usuarios en BD:", users.map(u => ({_id: u._id, name: u.name, email: u.email})));

  const rolesCollection = db.collection('roles');
  const adminRole = await rolesCollection.findOne({ name: 'ADMINISTRADOR_SISTEMA' });
  console.log("Admin Role:", adminRole);

  const userRolesCollection = db.collection('user_roles');
  const userRoles = await userRolesCollection.find({}).toArray();
  console.log("Roles asignados:", userRoles);

  mongoose.disconnect();
}
run().catch(console.error);
