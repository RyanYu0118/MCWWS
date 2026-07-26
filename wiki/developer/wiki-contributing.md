---
title: Wiki 贡献与待完善条目
slug: developer/wiki-contributing
description: 如何维护 Wiki、建设中页面列表。
categories: [Wiki, 开发者]
tags: [Wiki]
---

# Wiki 贡献与待完善条目

## Halo 交互页（与 demo 同系）

- 共享样式与脚本：`wiki/_halo/mcwws-wiki.css`、`wiki/_halo/mcwws-wiki.js`
- 在页面 frontmatter 之后按 **demo 顺序** 插入多块（每块可单独一行 UUID）：
  1. `{{WANDER_DEMO_NAV_CSS}}` + **HTML** 导航（`.nav-quote-box`）
  2. `{{WANDER_DEMO_CARD_CSS}}` + **HTML** 卡片（`#wanderCard` 的 `.wd-smart-card` 结构，见 `home.md` / `demo/流浪世界服务器Wiki.md`）
  3. `{{WANDER_DEMO_CARD_JS}}` — 与 demo 相同的 hover / 视频交互
  4. 可选 `{{MCWWS_HALO_CSS}}` / `{{MCWWS_HALO_JS}}` 做次级入口网格
- 子专题页可继续用纯 Markdown；需要卡片/步骤条时复制上述结构即可

## 待完善

（正文待撰写。个人草稿请放在站内草稿区或本仓库 PR，勿挂在全站首页。）

[← 开发者文档](index.md)
