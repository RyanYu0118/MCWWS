# 流浪世界 Wiki（仓库内 Markdown）

导入 RYAN STUDIO 时，建议按目录上传；各文件头部的 `slug` 与路径一致，便于在站内还原层级。

## 目录结构

```
wiki/
├── home.md                 # 全站首页（玩家 / 开发者两个入口）
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
