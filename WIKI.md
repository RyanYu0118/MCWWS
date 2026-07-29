# Wiki 源文件（已迁出）

流浪世界 Wiki 的 **Markdown 源稿**、`_halo/` 片段与 **Halo 发布脚本** 不再放在本服务器目录，统一维护于：

**[RyanYu0118/RYAN-STUDIO](https://github.com/RyanYu0118/RYAN-STUDIO)**

| 内容 | 在 RYAN-STUDIO 中的路径 |
|------|-------------------------|
| 文章与结构 | `wiki/**/*.md` |
| Halo 嵌入块 | `wiki/_halo/` |
| 编译 / 推送 | `tools/mcwws-halo-preview/`、`wiki/推送到Halo.ps1` 等 |

本地 Halo 站点资源（`rs-loader.js`、`fronts.css` 等）在同仓库的 `1panel/apps/halo/halo/data/attachments/upload/wiki-data/`。

请在 RYAN-STUDIO 工作区编辑 Wiki；本目录下的 `wiki/` 若存在，仅为旧文件残留，**勿提交**（已在 `.gitignore` 忽略）。
