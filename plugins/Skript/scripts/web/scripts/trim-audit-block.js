const fs = require('fs');
const p = require('path').join(__dirname, '..', 'public', 'manage', 'shop-locations.html');
const lines = fs.readFileSync(p, 'utf8').split(/\r?\n/);
const start = lines.findIndex(l => l.includes('loadAudit_REMOVED'));
const end = lines.findIndex((l, i) => i > start && l === '        document.addEventListener(\'keydown\', e => {');
if (start < 0 || end < 0) {
    console.error('markers', start, end);
    process.exit(1);
}
const commentStart = start > 0 && lines[start - 1].includes('审计') ? start - 1 : start;
const out = [...lines.slice(0, commentStart), ...lines.slice(end)];
fs.writeFileSync(p, out.join('\n'));
console.log('ok', commentStart, end);
