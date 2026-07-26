# 流浪世界 Wiki（仓库内 Markdown）

导入 RYAN STUDIO 时，建议按目录上传；各文件头部的 `slug` 与路径一致，便于在站内还原层级。

## 目录结构


```
wiki/
├── home.md                 # 全站首页（玩家 / 开发者两个入口）
├── demo/                   # 平台导出样例（含 HTML/CSS/JS 块，勿直接当站内首页）
│   └── 流浪世界服务器Wiki.md
├── player/                 # 玩家向
│   ├── index.md            # 玩家 Wiki 首页
│   ├── getting-started.md
│   ├── rules.md
│   ├── commands.md
│   ├── economy.md
│   ├── map-teleport.md
│   ├── server-world/       # 服务器世界（勿用 world/，会被 .gitignore 忽略）
│   └── community/          # 项目与日记
└── developer/              # 开发者 / 运维向
    ├── index.md
    ├── guide.md
    ├── wiki-contributing.md
    ├── resources/          # 推荐与在用的资源
    └── planning/           # 长期规划、软硬件
```

## Slug 约定

| 文件 | slug |
|------|------|
| `home.md` | `home` |
| `player/index.md` | `player` |
| `player/getting-started.md` | `player/getting-started` |
| `developer/index.md` | `developer` |
| … | 与相对路径相同，去掉 `.md` |

站内链接若不支持相对路径，导入后把正文中的 `(getting-started.md)` 改为平台要求的 `(player/getting-started)` 即可。

## 自定义 HTML 页面（`demo/`）

RYAN STUDIO / 站内编辑器支持**特殊块**：在 Markdown 中插入 UUID 行 + 原始 **CSS**、**HTML**、**`(function(){ ... })();` 脚本**（见 `demo/流浪世界服务器Wiki.md`）。可做出导航条、全息卡片、懒加载视频、深浅色适配等与纯 Markdown 无关的版式。

- **用途**：首页横幅、专题落地页、复杂交互组件。
- **注意**：脚本与样式仅在 Wiki 平台开启「原始 HTML」或对应块类型时生效；本仓库 Markdown 在 GitHub 预览里**不会**执行这些块。
- 维护：复杂页面可先在 `demo/` 留导出备份；**站内首页与 player/developer 索引** 使用 `{{MCWWS_HALO_CSS}}` / `{{MCWWS_HALO_JS}}` + `wiki/_halo/` 共享样式（见 `home.md`）

## 为什么在 Cursor 里「读」不出 Halo 效果？

Halo / RYAN STUDIO 的 **Markdown·HTML 混合块**（见 [Halo Markdown/HTML 内容块插件](https://www.halo.run/store/apps/app-NgHnY)）在存盘时往往是：

- 单独一行的 **UUID**（编辑器块 ID，不是正文）
- **未包在 \`\`\` 代码围栏里** 的 CSS、HTML、`<script>` / IIFE 脚本
- 站内由主题 + 插件渲染；**不是** GitHub 式标准 Markdown

因此 Cursor / VS Code 会出现：

| 现象 | 原因 |
|------|------|
| **预览** 只有乱糟糟的文字、无卡片/视频 | 内置预览只认 CommonMark/GFM，**不会**执行脚本、也不会应用裸 CSS |
| **编辑器** 一大坨单行 CSS，难以阅读 | 语法高亮仍按 `.md` 解析，块边界与 Halo 不一致 |
| **AI 对话** 若说「读不懂页面」 | 文件其实是纯文本，能读内容，但**无法从仓库还原站内排版**（缺 Halo 运行时） |

这不是文件损坏，而是 **渲染环境不同**。

### 在 Cursor 里怎么弄（推荐）

1. **日常文档**（规则、经济、命令）：只改 `wiki/player/`、`wiki/developer/` 等**标准 Markdown**（带 `slug` 头），在 Cursor 里预览、diff、给 AI 看都正常。
2. **复杂首页 / 专题**（如 `demo/流浪世界服务器Wiki.md`）：
   - **编辑**：在 **Halo / RYAN STUDIO 后台** 用 HTML / Markdown 块改，保存后 **导出或复制** 到 `wiki/demo/` 作备份；或在本机用 **HTML 语法高亮** 看代码（见下）。
   - **本地预览**：在浏览器打开 **已发布的 Wiki 页面**，或 Halo 后台的预览；不要指望 Cursor 的 Markdown 预览等于站内效果。
3. **可选**：仓库已配置 `wiki/demo/**` 在 Cursor 中按 **HTML** 高亮（见 `.vscode/settings.json` 的 `files.associations`），便于改 CSS/标签；**提交前**若需贴回 Halo，仍按平台要求导出（围栏与 UUID 以站内为准）。
4. **双文件**（可选）：`foo.md` 给 Git/AI，`foo.halo-export.md` 存 Halo 原样导出，避免互相覆盖。

**要看最终网页**：以 Halo / RYAN STUDIO 线上或后台预览为准；Cursor 负责改**标准 MD** 和**当代码库看** Halo 导出即可。

### 在 Cursor 里本地预览（近似站内）

1. 终端或 **Ctrl+Shift+P → Tasks: Run Task →「Halo: 编译 demo 预览 HTML」**（亦可在 `wiki/demo/` 运行 `预览Halo页.ps1`）。
2. 生成 `wiki/demo/_preview/<文件名>.html`（已 gitignore）。
3. **Ctrl+Shift+P → Simple Browser: Show**，粘贴任务输出里的 `file:///.../_preview/....html` 地址（需联网加载 marked.js CDN）。
4. 改完 `.md` 后**重新运行编译**再刷新 Simple Browser。

局限：导出里缺的 HTML 块（如仅留脚本无 `#wanderCard` 结构）会用占位卡片；`/upload/` 图片走 `wiki/demo/upload/`；视频/字体与线上一致性不保证。
