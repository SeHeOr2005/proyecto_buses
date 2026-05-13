const mongoose = require('mongoose');

const uri = 'mongodb+srv://juannaranjo56321:Leondelcovid19.@clusternarver.pwnrxr8.mongodb.net/test?appName=ClusterNarver';

async function run() {
  await mongoose.connect(uri);
  const db = mongoose.connection.db;

  const collections = await db.listCollections().toArray();
  console.log("Colecciones en test:", collections.map(c => c.name));

  for (let c of collections) {
    if (c.name.includes('user') || c.name.includes('User')) {
      const docs = await db.collection(c.name).find({}).toArray();
      console.log(`Documentos en ${c.name}: ${docs.length}`);
      console.log(docs.map(u => u.email));
    }
  }

  mongoose.disconnect();
}
run().catch(console.error);
