const fs = require('fs');
const s = fs.readFileSync('d:/Minecraft/服务器/1.21.11/bluemap/web/assets/index-ZbCQNBFs.js', 'utf8');
const i = s.indexOf('class MapHeightControls');
console.log(s.slice(i, i + 2500));
const j = s.indexOf('MapControls.getMaxPerspectiveAngleForDistance');
console.log('\n--- static method context ---');
console.log(s.slice(j - 500, j + 1500));
