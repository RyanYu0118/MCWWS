const fs = require('fs');
const p = require('path').join(__dirname, '..', 'public', 'manage', 'shop-locations.html');
let h = fs.readFileSync(p, 'utf8');
const s = h.indexOf('                <motion class="search-container">');
const searchAlt = h.indexOf('                <div class="search-container">');
const start = s >= 0 ? s : searchAlt;
const scriptStart = h.indexOf('    <script>');
if (start < 0 || scriptStart < 0) {
    console.error('markers not found', start, scriptStart);
    process.exit(1);
}
h = `${h.slice(0, start)}            </div>
        </div>
    </div>

${h.slice(scriptStart)}`;
h = h.replace(/<span class="brand-text">[\s\S]*?<\/span>/, '<span class="brand-text">流浪世界 · 管理</span>');
h = h.replace(/async function showApp\(\) \{[\s\S]*?loadSpecialItems\(\);\s*\}/m, `async function showApp() {
            document.getElementById('authGate').style.display = 'none';
            document.getElementById('app').style.display = 'block';
            startAdminAccessPolling();
            loadShopLocations();
        }`);
h = h.replace(/function exitAdminPage\(\) \{[\s\S]*?\}/m, `function exitAdminPage() {
            window.location.href = '../home.html';
        }`);
fs.writeFileSync(p, h);
console.log('trimmed ok', fs.statSync(p).size);
