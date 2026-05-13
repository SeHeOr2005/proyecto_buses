const mongoose = require('mongoose');

const uri = 'mongodb+srv://juannaranjo56321:Leondelcovid19.@clusternarver.pwnrxr8.mongodb.net/db_security?appName=ClusterNarver';

async function run() {
  await mongoose.connect(uri);
  const db = mongoose.connection.db;

  const collections = await db.listCollections().toArray();
  console.log("Colecciones en db_security:", collections.map(c => c.name));

  for (let c of collections) {
    const docs = await db.collection(c.name).find({}).toArray();
    console.log(`Documentos en ${c.name}: ${docs.length}`);
    if (c.name.includes('user')) {
       console.log(docs);
    }
  }

  mongoose.disconnect();
}
run().catch(console.error);
