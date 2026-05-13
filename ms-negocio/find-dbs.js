const mongoose = require('mongoose');

const uri = 'mongodb+srv://juannaranjo56321:Leondelcovid19.@clusternarver.pwnrxr8.mongodb.net/?appName=ClusterNarver';

async function run() {
  await mongoose.connect(uri);
  const admin = mongoose.connection.db.admin();
  const dbs = await admin.listDatabases();
  console.log("Databases:", dbs.databases.map(d => d.name));
  
  for (let dbInfo of dbs.databases) {
    const db = mongoose.connection.useDb(dbInfo.name);
    const users = await db.collection('user').find({}).toArray();
    if (users.length > 0) {
      console.log(`Encontrados ${users.length} usuarios en la base de datos: ${dbInfo.name}`);
      console.log(users.map(u => u.email));
    }
    const oldUsers = await db.collection('users').find({}).toArray();
    if (oldUsers.length > 0) {
      console.log(`Encontrados ${oldUsers.length} 'users' en la base de datos: ${dbInfo.name}`);
      console.log(oldUsers.map(u => u.email));
    }
  }

  mongoose.disconnect();
}
run().catch(console.error);
