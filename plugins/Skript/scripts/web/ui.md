# MCWWS 网页服务 UI 设计规范

> **适用范围：** `plugins/Skript/scripts/web/public/` 下的流浪世界网页服务（商店、地图壳页、零钱明细、管理、PWA、Android WebView 壳）。  
> **公开地址：** https://mcs.ryanstudio.work/  
> **风格代号：** iOS 26 液态玻璃（Liquid Glass）+ Minecraft 像素字体  
> **文档版本：** 与 `style.css` / `themes.css` 同步（`DS-WEB-VERSION` 见各 HTML 注释）

维护者改样式时：**先改 CSS 实现，再改本文与 `WIKI.md` 附录 G**。

---

## 1. 设计原则

| 原则 | 说明 |
| ---- | ---- |
| 玻璃拟态 | 半透明叠层 + `backdrop-filter` 模糊 + 内描边高光，模拟 iOS 26 液态玻璃 |
| 灰阶主色 | 主交互为黑白灰渐变，状态色（成功/危险/警告）仅用于数据语义 |
| 无粗体标题 | 全局 `font-weight: normal !important`，靠字号与渐变字区分层级 |
| Minecraft 气质 | 正文字体 `MinecraftFont`（`5_Minecraft AE.ttf`）；时钟等数字可用 `MinecraftAE` |
| 深/浅双主题 | `html[data-color-scheme="dark\|light"]`，默认深色；`localStorage: mcwws.web.colorScheme` |
| 动效克制 | 页面切换 280ms、主题切换 320ms；尊重 `prefers-reduced-motion: reduce` |
| 移动优先断点 | `≤768px` 平板、`≤640px` 手机；顶栏与物品网格有专项规则 |

---

## 2. 样式文件分层

| 文件 | 职责 |
| ---- | ---- |
| `style.css` | 布局、组件结构、商店/仪表板/模态框等业务样式；定义 `:root` **基础** token（部分会被 themes 覆盖） |
| `themes.css` | **生效中的** 深/浅色 token、玻璃增强、噪点背景、主题切换器、View Transition |
| `mcwws-auth.css` | 登录头像、Popover、经济信息卡片（商店/地图/管理页可单独引用） |
| `mcwws-theme.js` | 主题读写、切换动画 overlay |
| `mcwws-page-transition.js` | 站内链接切换遮罩（280ms）与 prefetch |
| `mcwws-pwa.js` | PWA 注册、`theme-color` 同步 |

**加载顺序（典型页面）：** `style.css` → `mcwws-theme.js`（head 早期）→ `themes.css` → 业务脚本。

> **注意：** 圆角、玻璃模糊、背景色等以 **`themes.css` 中 `[data-color-scheme]` 的值为准**；`style.css` 的 `:root` 为未加载 themes 时的回退。

---

## 3. 设计 Token（生效值）

### 3.1 圆角（Border Radius）

| Token | 深色/浅色（themes.css） | style.css 回退 | 典型用途 |
| ----- | ---------------------- | -------------- | -------- |
| `--radius-sm` | **12px** | 8px | 导航链接（非 pill）、输入次级、标签、排行榜项、洞察卡片 |
| `--radius-md` | **18px** | 12px | 趋势项、排行榜头像、统计图标容器、物品价格条 |
| `--radius-lg` | **24px** | 16px | 玻璃卡片、统计卡、模态内容（小屏）、时钟面板、零钱弹层 |
| `--radius-xl` | **32px** | 24px | 服务模块大卡、模态默认外框、建造工具卡 |
| **999px** | 固定 pill | 同左 | 主按钮、购物车按钮、主题切换、Tab、数量徽章、安装提示按钮 |
| **10px** | 固定 | 同左 | 登录头像按钮、物品模态统计格 `.item-modal-stat` |
| **14px** | 固定 | 同左 | 登录表单 `input` |
| **16px** | 固定 | 同左 | 服务模块图标底 `.services-module-icon` |
| **50%** | 圆形 | 同左 | 刷新按钮、关闭按钮、排名徽章、购物车角标按钮 |

**硬编码圆角一览（未走 token 的组件）：**

| 像素 | 选择器 / 场景 |
| ---- | ------------- |
| 4px | 滚动条 thumb |
| 8px | `.shop-item-card-prices`、`.mcwws-auth-popover-action`、管理页部分输入 |
| 12px | `.shop-item-card`、`.mcwws-auth-popover-body` |
| 3px | 物品卡顶部 accent 条高度（非圆角，高 3px） |

### 3.2 间距（Spacing）

基准：多数使用 `rem`（根字号 16px），部分移动端改用 `px` 保证触控。

| 场景 | 数值 |
| ---- | ---- |
| 页面容器 `.container` | `padding: 2rem`（32px）；`≤768px` → `1rem` |
| 服务首页主区 `.services-hub-main` | 左右 `1.5rem`，底 `3rem`；`max-width: 1100px` |
| 模块卡片 `.services-module-card` | `padding: 1.75rem 1.5rem`（28px 24px）；`gap: 0.75rem` |
| 模块网格 `.services-module-grid` | `gap: 1.25rem`；列 `minmax(280px, 1fr)` |
| 导航栏 `.navbar` | 高 **64px**；左右 `2rem`；`≤768px` 隐藏横链；`≤640px` 最小高 **56px**、可折行 |
| 顶栏下首屏 `.hero` | `margin-top: 64px`；`padding: 4rem 2rem 3rem` |
| 模态 `.modal` | 外边 `padding: 1rem`；头/体 `1.5rem` |
| 模态 `.modal-body` | `max-height: 70vh` |
| 表单字段 `.form-field` | 标签与输入 `gap: 0.35rem`；字段间距 `margin-bottom: 1rem` |

### 3.3 颜色 — 深色主题（默认）

| Token | 值 | 用途 |
| ----- | -- | ---- |
| `--bg-primary` | `#050505` | 页面底、PWA `theme-color`、Android 启动背景 |
| `--bg-secondary` | `rgba(255,255,255,0.06)` | 次级面板 |
| `--bg-tertiary` | `rgba(255,255,255,0.10)` | 悬停、输入底、刷新按钮 |
| `--bg-card` | `rgba(255,255,255,0.06)` | 卡片填充 |
| `--text-primary` | `rgba(255,255,255,0.96)` | 正文 |
| `--text-secondary` | `rgba(255,255,255,0.78)` | 副文案 |
| `--text-muted` | `rgba(255,255,255,0.52)` | 说明、页脚 |
| `--primary` / `--primary-light` | `#f5f5f5` / `#ffffff` | 强调、渐变字 |
| `--on-primary` | `#0a0a0a` | 主按钮文字、排名圆底字 |
| `--border` | `rgba(255,255,255,0.14)` | 分割线 |
| `--border-glow` | `rgba(255,255,255,0.22)` | 卡片 hover 描边 |
| `--success` | `#30D158` | 涨跌、成功 |
| `--danger` | `#FF453A` | 错误、卖价、关闭 hover |
| `--warning` | `#FFD60A` | 经济标题 |
| `--glass-bg` | `rgba(255,255,255,0.08)` | 玻璃底层 |
| `--glass-border` | `rgba(255,255,255,0.18)` | 玻璃边 |
| `--glass-blur` | **32px** | `backdrop-filter` 模糊半径 |
| `--modal-overlay` | `rgba(0,0,0,0.55)` | 模态遮罩 |
| `--navbar-bg` | `rgba(255,255,255,0.06)` | 顶栏 |

**语义硬编码色（登录 Popover）：**

| 用途 | 色值 |
| ---- | ---- |
| 经济标签绿 | `#86efac` |
| 余额黄 | `#fde047` / 更新闪烁 `#fef08a` |
| OP 金 | `#fcd34d` |
| 进度青 | `#67e8f9` |
| 登出字 | `#fca5a5` |
| 买价（模态） | `#34d399` |
| 卖价（模态） | `#f87171` |

### 3.4 颜色 — 浅色主题

| Token | 值 |
| ----- | -- |
| `--bg-primary` | `#ebedf2` |
| `--bg-secondary` | `#f2f3f6` |
| `--bg-tertiary` | `#f7f8fa` |
| `--bg-card` | `#fafbfc` |
| `--text-primary` / `--text-secondary` / `--text-muted` | 均为 `#000000`（高对比纯黑策略） |
| `--glass-blur` | **32px** |
| `--modal-overlay` | `rgba(0,0,0,0.35)` |
| 主题切换 overlay | `#ebedf2`（`mcwws-theme.js`） |

### 3.5 阴影

| Token | 深色 | 浅色 |
| ----- | ---- | ---- |
| `--shadow-sm` | `0 2px 12px rgba(0,0,0,0.28)` | `0 2px 12px rgba(0,0,0,0.05)` |
| `--shadow-md` | `0 8px 28px rgba(0,0,0,0.32)` | `0 8px 28px rgba(0,0,0,0.06)` |
| `--shadow-lg` | `0 16px 48px rgba(0,0,0,0.38)` | `0 16px 48px rgba(0,0,0,0.08)` |
| `--shadow-glow` | `0 0 48px rgba(255,255,255,0.04)` | `0 0 48px rgba(0,0,0,0.04)` |
| `--navbar-shadow` | `0 4px 24px rgba(0,0,0,0.25)` | `0 4px 24px rgba(0,0,0,0.06)` |

玻璃 `.glass` 额外：`inset 0 1px 0 var(--glass-inset)` + `inset 0 -1px 0 rgba(255,255,255,0.03)`。

### 3.6 渐变

| Token | 深色 |
| ----- | ---- |
| `--gradient-primary` | `linear-gradient(135deg, #ffffff 0%, #a3a3a3 100%)` |
| `--gradient-hero` | `linear-gradient(180deg, #050505 0%, #0f0f0f 50%, #050505 100%)` |
| `--gradient-card` | `linear-gradient(145deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.02) 100%)` |
| `--body-bg-backdrop` | 三层径向高光 + 165° 线性底（见 `themes.css`） |

### 3.7 动效时长

| Token / 场景 | 时长 | 缓动 |
| ------------ | ---- | ---- |
| `--transition-fast` | **150ms** | ease |
| `--transition-normal` | **300ms** | ease |
| `--transition-slow` | **500ms** | ease |
| 主题切换 | **320ms** | ease（`--theme-transition-duration`） |
| 页面切换遮罩 | **280ms** | ease（`MCWWS_PAGE_TRANSITION_MS`） |
| 模态入场 | 背景 **220ms**；卡片 **280ms** | `cubic-bezier(0.16, 1, 0.3, 1)` |
| 模态退场 | **180ms** | ease |
| Popover 显隐 | **150ms** opacity；visibility 延迟 **280ms** 关闭 |

---

## 4. 字体与排版

### 4.1 字体族

```text
MinecraftFont, 'Segoe UI', sans-serif   /* 全局强制，含标题 */
Inter, system-ui                          /* body 声明但常被 !important 覆盖 */
MinecraftAE                               /* 仪表板时钟数字 */
ui-monospace, monospace                   /* 模态统计数值 */
```

- 文件：`public/5_Minecraft AE.ttf`
- `unicode-range` 排除 `U+00D7`（×），避免 Minecraft 字体缺字

### 4.2 字号阶梯（rem → 16px 基准）

| 级别 | 尺寸 | 示例 |
| ---- | ---- | ---- |
| 英雄标题 | `3rem`（48px）→ 移动 `1.75rem` | `.hero-title` |
| 服务首页标题 | `clamp(2rem, 5vw, 3rem)` | `.services-hub-title` |
| 模态标题 | `1.5rem`（24px）→ 小屏 `1.15rem` | `.modal-header h2` |
| 模块名 | `1.35rem`（21.6px） | `.services-module-name` |
| 统计值 | `1.75rem`（28px） | `.stat-value` |
| 品牌字 | `1.25rem`（20px）→ 手机 `0.95rem` | `.brand-text` |
| 正文 | `1rem` / `0.92rem` | 模块描述、表单 |
| 辅助 | `0.85rem` ~ `0.75rem` | 页脚、上次更新、标签 |
| 微字 | `0.72rem` | 经济 Popover 标题（大写） |

### 4.3 行高

- 正文默认：`line-height: 1.6`
- 紧凑 UI：`1.35` ~ `1.55`（卡片、Popover）

---

## 5. 布局与栅格

| 区域 | 规则 |
| ---- | ---- |
| 主内容最大宽 | `.container` **1400px**；仪表板 hero **72rem**；服务 hub **1100px** |
| 统计网格 | `repeat(auto-fit, minmax(...))`；`≤768px` 单列 |
| 物品目录网格 | 桌面多列；`≤640px` 专项（见 `style.css` 物品目录段） |
| 地图页 | `iframe` 全屏；`.services-map-auth-float` 浮层登录 |
| 固定顶栏 | 高 64px，`z-index: 1000`；内容区 `margin-top: 64px` |

---

## 6. Z-index 层级

| 值 | 用途 |
| -- | ---- |
| `-2` / `-1` | `body::before` 背景渐变 / 噪点纹理 |
| `0` ~ `2` | 卡片内部 accent |
| `80` | `admin.html` 内嵌管理顶栏 |
| `100` | 管理页浮动元素 |
| `200` | 管理页 toast |
| `1000` | 主站 `.navbar` |
| `2000` | `.modal` 登录/交易模态 |
| `9999` | 浮动主题切换器 `.mcwws-theme-switcher--float` |
| `12000` | `.mcwws-auth-popover`、`.mcwws-ledger-modal` |
| `2147483645` | 主题切换全屏 overlay |
| `2147483646` | 页面切换遮罩 `html.mcwws-pt-curtain::after` |

---

## 7. 核心组件规范

### 7.1 顶栏 `.navbar`

- 尺寸：高 **64px**；左右 padding **32px**（`2rem`）
- 背景：`--navbar-bg` + `blur(40px) saturate(120%)`（themes 增强）
- 链接 `.nav-link`：padding `8px 16px`；圆角 **12px**（themes 下 **999px** pill）
- 激活：浅白底 `rgba(255,255,255,0.12)`
- 刷新 `.refresh-btn`：**40×40px** 圆形

### 7.2 玻璃容器 `.glass`

- 使用 token 渐变填充 + `--glass-blur` **32px** + `saturate(120%)`
- 边框 `1px solid var(--glass-edge)`
- 禁止在 `.shop-item-card` 上强依赖 backdrop（已改实底 `--bg-card` 防 1px 闪线）

### 7.3 服务模块卡 `.services-module-card`

- 圆角 **`--radius-xl`（32px）**
- 内边距 **28px 24px**；图标区 **56×56px**，圆角 **16px**
- Hover：`translateY(-4px)` + `--shadow-glow`

### 7.4 商店物品卡 `.shop-item-card`

- 圆角 **12px**；padding **20px**
- 顶条 accent：高 **3px**，水平渐变
- 价格区：圆角 **8px**；padding **12px**

### 7.5 按钮

| 类名 | 尺寸 / 圆角 | 视觉 |
| ---- | ----------- | ---- |
| `.primary-btn` | padding `13.6px 20px`；**999px** | `--gradient-primary` 底 + 深色字 |
| `.trade-btn` / `.cart-btn` | padding `10px 18px`；**999px** | active 同主按钮；disabled `#334155` |
| `.auth-button` | padding `10.4px 16px`；**999px** | 半透明白底 |
| `.cart-offer-add-btn` | padding `8px 16px`；**999px**；字 `0.88rem` | 主渐变 |
| `.services-hub-install-btn` | padding `4px 13.6px`；**999px**；字 `0.82rem` | 蓝 `#3d7eff` 回退 |

### 7.6 表单 `.form-field`

- 标签：`0.9rem`，`--text-secondary`
- 输入：padding **14.4px 16px**；圆角 **14px**；底 `rgba(14,14,14,0.9)`
- Focus：边框 `rgba(255,255,255,0.45)` + 外发光 `0 0 0 3px rgba(255,255,255,0.1)`

### 7.7 模态框 `.modal`

- 遮罩：入场至 `rgba(0,0,0,0.8)`
- 内容：max-width **800px**；max-height **90vh**；圆角 **`--radius-xl`**
- 关闭钮：**40×40px** 圆；hover `--danger`

### 7.8 登录 `.mcwws-auth-*`

- 头像钮：**40×40px**；圆角 **10px**；边框 **2px**
- Popover：min-width **220px** max **300px**；卡片圆角 **12px**；padding **12px 14px**
- 箭头：**10×10px** 旋转方块
- 经济进度条：高 **5px**；圆角 **999px**

### 7.9 主题切换 `.mcwws-theme-toggle`

- 外框高 **36px**；pill **999px**；内图标区 **28×28px**
- 未选中图标 `opacity: 0.38`；选中 `1` + `scale(1.05)`

### 7.10 滚动条（WebKit）

- 宽/高：**8px**
- thumb 圆角：**4px**

---

## 8. 背景与纹理

- `body::before`：多层 `--body-bg-backdrop` 固定全屏
- `body::after`：SVG 噪点平铺 **180×180px**；深色 `opacity: 0.22` overlay；浅色 `0.06` soft-light
- 服务 hero 光晕：椭圆高 **420px**，`inset: -40% -20% auto`

---

## 9. 响应式断点

| 断点 | 主要变化 |
| ---- | -------- |
| `≤768px` | 隐藏横排 `.nav-links`；hero/仪表板单列；`.container` 缩进；统计/内容网格单列 |
| `≤640px` | 顶栏折行、底栏式三列导航；`body` 字号 **14px**；品牌字省略；hero 顶 padding **7.7rem**；模态 `96vw`；触控最小高 **44px** 搜索框 |
| `prefers-reduced-motion` | 关闭模态/主题/页面过渡动画 |
| `display-mode: standalone` | 隐藏 PWA 安装提示与 APK 卡片 |

---

## 10. 页面模块对照

| 页面 | 文件 | 备注 |
| ---- | ---- | ---- |
| 服务首页 | `home.html` | 模块入口；PWA/APK 提示 |
| 商店仪表板 | `index.html` | 统计卡、图表、时钟 widget |
| 物品目录 | `items.html` | 字母索引、购物车、物品卡 |
| 地图壳 | `map.html` | BlueMap iframe + 浮层登录 |
| 零钱明细 | `ledger.html` | 账本表格 |
| 管理 | `manage/shop-locations.html`、`admin.html` | 后者为独立深色管理皮肤 |
| 离线 | `offline.html` | PWA 断网页；底 `#050505` |

**Android App（`tools/mcwws-web-android`）** 在 WebView 内注入 CSS 隐藏「更多服务」链接与安装提示；底部原生 Tab 高 **56dp**，强调色 `#3D7EFF`。

---

## 11. PWA / 浏览器 Chrome

| 项 | 值 |
| -- | -- |
| `theme-color` 深色 | `#050505` |
| `theme-color` 浅色 | `#ebedf2` |
| `background_color` | `#050505` |
| 图标 | `192` / `512` + maskable 变体（`public/icons/`） |
| 显示模式 | `standalone` |

---

## 12. 开发约定

1. **优先用 CSS 变量**，避免新增硬编码色；若必须硬编码，在本文补充。
2. **圆角优先用 token**（`var(--radius-*)`），pill 统一 **999px**。
3. **新页面**须引入：`style.css` + `themes.css` + `mcwws-theme.js`；需登录则加 `mcwws-auth.css`。
4. **站内链接**保持相对路径，以启用 `mcwws-page-transition.js`。
5. **勿对 `body` 使用 `transform`**，以免 `position: fixed` 顶栏错位（页面过渡已用 `html::after` 遮罩）。
6. **缓存破坏**：改 CSS 时递增 HTML 中 `?v=` 查询参数。
7. **管理页 `admin.html`** 使用独立 CSS 变量（`--accent-glow` 等），与主站 token 相近但不完全共用 class。

---

## 13. 源码索引

| 主题 | 路径 |
| ---- | ---- |
| 主样式 | `public/style.css` |
| 主题 token | `public/themes.css` |
| 登录组件 | `public/mcwws-auth.css` |
| 主题脚本 | `public/mcwws-theme.js` |
| 页面过渡 | `public/mcwws-page-transition.js` |
| PWA | `public/manifest.webmanifest`、`public/mcwws-pwa.js`、`public/sw.js` |
| Android 壳 | `tools/mcwws-web-android/` |

---

*最后更新：2026-08-29 · 对应 Web 样式版本 `style.css v2.5.91` / `themes.css v18` 量级*
