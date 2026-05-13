const mongoose = require('mongoose');

const uri = 'mongodb+srv://juannaranjo56321:Leondelcovid19.@clusternarver.pwnrxr8.mongodb.net/db_security?appName=ClusterNarver';

async function run() {
  await mongoose.connect(uri);
  const db = mongoose.connection.db;

  const usersCollection = db.collection('user');
  const users = await usersCollection.find({}).toArray();
  console.log("Usuarios en BD (user):", users.map(u => ({_id: u._id, name: u.name, email: u.email})));

  const rolesCollection = db.collection('role');
  const adminRole = await rolesCollection.findOne({ name: 'ADMINISTRADOR_SISTEMA' });
  console.log("Admin Role:", adminRole);

  const userRolesCollection = db.collection('userRole');
  const userRoles = await userRolesCollection.find({}).toArray();
  console.log("Roles asignados:", userRoles);

  if (users.length > 0 && adminRole) {
    const userToMakeAdmin = users[0]; // Toma el primero
    // check if it already has the role
    const hasRole = await userRolesCollection.findOne({ "user.$id": userToMakeAdmin._id, "role.$id": adminRole._id });
    if (!hasRole) {
      // Create userRole
      await userRolesCollection.insertOne({
        user: { $ref: "user", $id: userToMakeAdmin._id },
        role: { $ref: "role", $id: adminRole._id }
      });
      console.log(`Otorgado ADMINISTRADOR_SISTEMA a ${userToMakeAdmin.email}`);
    } else {
      console.log(`El usuario ${userToMakeAdmin.email} ya es admin.`);
    }
  }

  mongoose.disconnect();
}
run().catch(console.error);
