const fs = require('fs');
const lunr = require('../client/node_modules/lunr');

function makeLunrIndex(inputFile, outputFile) {
   const text = fs.readFileSync(inputFile, {encoding: 'UTF-8'});
   const docs = JSON.parse(text);

   const idx = lunr(function () {
      this.ref('i');
      this.field('t');
      this.field('b');

      docs.forEach(function (doc) {
         this.add(doc);
      }, this);
   });

   fs.writeFileSync(outputFile, JSON.stringify(idx));
}

process.argv.slice(2).forEach(dir => {
   makeLunrIndex(`${dir}/lunrData.json`, `${dir}/lunrIndex.json`);
});
