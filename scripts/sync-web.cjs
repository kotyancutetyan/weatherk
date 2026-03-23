const fs = require('fs');
const path = require('path');

const root = process.cwd();
const webDir = path.join(root, 'www');
const files = ['index.html', 'compliments.js', 'avatar.png', 'config.js'];

if (!fs.existsSync(webDir)) {
  fs.mkdirSync(webDir, { recursive: true });
}

for (const file of files) {
  const src = path.join(root, file);
  const dst = path.join(webDir, file);
  if (!fs.existsSync(src)) {
    // config.js может отсутствовать в репозитории на чистой машине
    continue;
  }
  fs.copyFileSync(src, dst);
}

console.log('Synced web files to www/');
