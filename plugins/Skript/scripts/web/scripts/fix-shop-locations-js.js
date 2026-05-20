const fs = require('fs');
const path = require('path');

const adminPath = path.join(__dirname, '..', 'public', 'admin.html');
const shopPath = path.join(__dirname, '..', 'public', 'manage', 'shop-locations.html');

const admin = fs.readFileSync(adminPath, 'utf8');
let shop = fs.readFileSync(shopPath, 'utf8');

function sliceBetween(src, startMarker, endMarker) {
    const start = src.indexOf(startMarker);
    const end = src.indexOf(endMarker, start);
    if (start < 0 || end < 0) throw new Error(`markers not found: ${startMarker}`);
    return src.slice(start, end);
}

let body = sliceBetween(admin, '        async function showApp()', '        // ─── 分类 ───');
body = body.replace(/loadShopLocations\(\);\s*loadCategories\(\);\s*loadConfig\(\);\s*loadSpecialItems\(\);/, 'loadShopLocations();');
body = body.replace(/window\.location\.href = 'index\.html'/, "window.location.href = '../home.html'");
body = body.replace(
    /titleEl\.textContent = accessDenied \? '权限不足' : '管理面板需要登录';/,
    "titleEl.textContent = accessDenied ? '权限不足' : '管理功能需要登录';"
);
body = body.replace(
    /messageEl\.textContent = message \|\| '请使用和仪表盘、物品目录一致的账户系统登录。登录后即可进入管理功能。';/,
    "messageEl.textContent = message || '请使用和商店系统一致的账户登录。登录后即可配置 UltimateShop 商店在 BlueMap 上的位置。';"
);

const tail = sliceBetween(admin, '        function toast(msg, type = \'info\')', '        document.querySelector(\'[data-tab="audit"]\')');

const scriptStart = shop.indexOf('        async function showApp()');
const scriptEnd = shop.indexOf('    </script>', scriptStart);
if (scriptStart < 0 || scriptEnd < 0) {
    console.error('script bounds not found');
    process.exit(1);
}

const footer = `
        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') {
                if (accessDeniedActive) return;
                closeAuthModal();
            }
        });

        if (window.location.protocol !== 'https:' && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
            document.getElementById('authSecurityWarning').innerHTML = \`
        <div style="background: rgba(239, 68, 68, 0.15); border: 1px solid var(--danger); color: #f87171; padding: 12px; border-radius: 8px; margin-bottom: 20px; font-size: 13px; text-align: left;">
            <strong>⚠️ 检测到不安全连接</strong><br>
            你正在通过未加密的 HTTP 连接访问管理功能。避免在公共网络下登录，以防止凭据被窃取。
        </motion>
    \`.replace(/<\\/motion>/, '</div>');
        }

        init();
`;

shop = shop.slice(0, scriptStart) + body + '\n\n' + tail + footer + shop.slice(scriptEnd);
fs.writeFileSync(shopPath, shop);
console.log('fixed', fs.statSync(shopPath).size);
