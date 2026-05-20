const fs = require('fs');
const path = require('path');

const pub = path.join(__dirname, '..', 'public');
const manage = path.join(pub, 'manage');

function toRoot(html) {
    return html
        .replace(/href="\.\.\/style\.css/g, 'href="style.css')
        .replace(/href="\.\.\/home\.html"/g, 'href="home.html"')
        .replace(/href="dashboard\.html"/g, 'href="index.html"')
        .replace(/src="\.\.\//g, 'src="')
        .replace(/"three": "\.\.\/vendor\//g, '"three": "./vendor/')
        .replace(/window\.location\.href = 'dashboard\.html'/g, "window.location.href = 'index.html'")
        .replace(/\s*<a href="index\.html" class="nav-link" title="管理模块首页">[\s\S]*?<\/a>\s*/g, '\n            ')
        .replace(/\s*<a href="admin\.html" class="nav-link[\s\S]*?<\/a>\s*/g, '\n            ');
}

const shopNav = `        <div class="nav-links">
            <a href="../home.html" class="nav-link" title="返回流浪世界服务">
                <span class="nav-icon">🏠</span> 服务
            </a>
        </div>`;

fs.writeFileSync(path.join(pub, 'items.html'), toRoot(fs.readFileSync(path.join(manage, 'items.html'), 'utf8')));

let indexHtml = toRoot(fs.readFileSync(path.join(manage, 'dashboard.html'), 'utf8'));
fs.writeFileSync(path.join(pub, 'index.html'), indexHtml);

let admin = fs.readFileSync(path.join(manage, 'admin.html'), 'utf8');
admin = admin.replace(/src: url\('\.\.\/5_Minecraft/g, "src: url('5_Minecraft");
admin = admin.replace(/href="\.\.\/home\.html"/g, 'href="home.html"');
admin = admin.replace(/href="dashboard\.html"/g, 'href="index.html"');
admin = admin.replace(/window\.location\.href = 'dashboard\.html'/g, "window.location.href = 'index.html'");
fs.writeFileSync(path.join(pub, 'admin.html'), admin);

let shopLoc = fs.readFileSync(path.join(manage, 'admin.html'), 'utf8');
shopLoc = shopLoc.replace(/src: url\('\.\.\/5_Minecraft/g, "src: url('../5_Minecraft");
shopLoc = shopLoc.replace(/<title>[\s\S]*?<\/title>/, '<title>管理 · UltimateShop 商店位置</title>');
shopLoc = shopLoc.replace(/流浪世界服务器商店系统管理面板/g, '流浪世界 · 管理');
shopLoc = shopLoc.replace(
    /<motion class="nav-links">[\s\S]*?<\/div>\s*<div class="nav-actions">/,
    `${shopNav}\n        <div class="nav-actions">`
);
shopLoc = shopLoc.replace(/<div class="nav-links">[\s\S]*?<\/div>\s*<div class="nav-actions">/, `${shopNav}\n        <div class="nav-actions">`);

shopLoc = shopLoc.replace(/<div class="nav-tabs">[\s\S]*?<\/motion>\s*<div class="content">/m, '<div class="content">');
shopLoc = shopLoc.replace(/<div class="nav-tabs">[\s\S]*?<\/div>\s*<div class="content">/, '<motion class="content">').replace(/<motion class="content">/, '<div class="content">');

// Remove other tab panels
shopLoc = shopLoc.replace(/<div class="tab-content" id="tab-config">[\s\S]*?<div class="tab-content" id="tab-actions">[\s\S]*?<motion class="tab-content" id="tab-perms">[\s\S]*?<div class="tab-content" id="tab-audit">[\s\S]*?<\/motion>\s*<\/div>\s*<\/motion>/m, '');
shopLoc = shopLoc.replace(/<motion class="tab-content" id="tab-config">[\s\S]*$/m, '');

// Simpler: remove from tab-config to end of content before modals - use marker
const configStart = shopLoc.indexOf('<motion class="tab-content" id="tab-config">');
if (configStart < 0) {
    const alt = shopLoc.indexOf('<div class="tab-content" id="tab-config">');
    if (alt >= 0) {
        const contentEnd = shopLoc.indexOf('</div>\n    </div>\n\n    <!-- 物品编辑', alt);
        if (contentEnd > alt) shopLoc = shopLoc.slice(0, alt) + shopLoc.slice(contentEnd);
    }
} else {
    const contentEnd = shopLoc.indexOf('</div>\n    </div>\n\n    <!-- 物品编辑', configStart);
    if (contentEnd > configStart) shopLoc = shopLoc.slice(0, configStart) + shopLoc.slice(contentEnd);
}

// Remove search and category/items inside tab-shop
shopLoc = shopLoc.replace(/\s*<div class="search-container">[\s\S]*?<div id="itemsView"[\s\S]*?<\/div>\s*<\/div>\s*<\/motion>\s*<\/motion>/m, '\n            </div>\n        </div>');
shopLoc = shopLoc.replace(/\s*<motion class="search-container">[\s\S]*?<\/div>\s*<\/div>\s*<\/div>\s*<\/div>/m, '\n            </div>\n        </div>');

const searchIdx = shopLoc.indexOf('<div class="search-container">');
const tabShopEnd = shopLoc.indexOf('</motion>\n            <div class="tab-content" id="tab-config">');
const tabShopEnd2 = shopLoc.indexOf('</div>\n            <div class="tab-content" id="tab-config">');
let cutAt = tabShopEnd2 > 0 ? tabShopEnd2 : shopLoc.indexOf('            </motion>\n            <!-- 分类视图 -->');
if (searchIdx > 0 && cutAt > searchIdx) {
    shopLoc = shopLoc.slice(0, searchIdx) + '\n            </div>\n        </motion>\n' + shopLoc.slice(cutAt).replace(/^[\s\S]*?<\/div>\s*<\/motion>/, '');
}

shopLoc = shopLoc.replace(/管理面板需要登录/g, '管理功能需要登录');
shopLoc = shopLoc.replace(/async function showApp\(\) \{[\s\S]*?loadSpecialItems\(\);\s*\}/,
    `async function showApp() {
            document.getElementById('authGate').style.display = 'none';
            document.getElementById('app').style.display = 'block';
            startAdminAccessPolling();
            loadShopLocations();
        }`);
shopLoc = shopLoc.replace(/function exitAdminPage\(\) \{[\s\S]*?\}/,
    "function exitAdminPage() {\n            window.location.href = '../home.html';\n        }");

// Remove tab click handlers content for removed tabs - optional
shopLoc = shopLoc.replace(/document\.querySelectorAll\('\.nav-tab'\)[\s\S]*?}\);\s*\n\n        \/\/ ─── UltimateShop/,
    '\n        // ─── UltimateShop');

fs.writeFileSync(path.join(manage, 'shop-locations.html'), shopLoc);

['items.html', 'dashboard.html', 'index.html', 'admin.html'].forEach((f) => {
    const p = path.join(manage, f);
    if (fs.existsSync(p)) fs.unlinkSync(p);
});
if (fs.existsSync(path.join(manage, 'index.html'))) fs.unlinkSync(path.join(manage, 'index.html'));

console.log('Done: items.html, index.html, admin.html, manage/shop-locations.html');
