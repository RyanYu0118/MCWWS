---
name: minecraft-builder
description: >-
  Designs and builds Minecraft Java structures on this MCWWS Paper server via
  MCWWS_BuildBridge MCP (set_block/fill/set_blocks, FAWE, schem_search), not
  mineflayer. Use when building or remodeling shops, landmarks, interiors,
  roads, cities, pasting schematics, or the user mentions 建造/装修/立面/投影/
  KFC/图书馆/街区/BuildBridge.
---

# Minecraft Builder（MCWWS / BuildBridge）

改编自公开 [minecraft-builder-skill](https://github.com/wzhaoMS/minecraft-builder-skill) 的构图与流程思路；**执行层全部换成本仓库 BuildBridge**。禁止另起 mineflayer bot、禁止 `nogui` 开服、禁止未同意的大范围清场。

硬性红线以始终生效的规则为准（本 skill 不重复展开）：

- `.cursor/rules/world-backup-build-safety.mdc`
- `.cursor/rules/build-interior-quality.mdc`
- `.cursor/rules/server-startup.mdc`

细节构图见同目录 `reference-patterns.md`。

## 写块通道（唯一默认）

MCP 命名空间：`mcwws-build-bridge`（本机 HTTP `127.0.0.1:8765`）。

| 用途 | 工具 |
| --- | --- |
| 定位 | `list_players` / `get_player_pos` / `get_block` |
| 直写 | `set_block` / `fill` / `set_blocks` |
| 直写撤销 | `write_undo` / `write_redo` / `write_history`（与 FAWE 无关；重启丢历史） |
| FAWE | `fawe_*`（`fawe_undo` ≠ `write_undo`） |
| 参考投影 | `schem_search` / `schem_info` / `schem_index_status` / `schem_reindex` |

批量脚本可放在 `tools/mcwws-build-bridge-mcp/*.mjs`，经同一 HTTP API 写块。新投影进库后：`npm run schem:index`（目录内）。

## 开工清单（每次建造）

1. **读玩家坐标** → 对照领地 / 邻近建筑 / 用户约束。
2. **报拟建 AABB**（x/y/z 含上下限）+ 说明避开什么；**用户未明确同意前不写块**。
3. **可感知建筑先 `schem_search`**（店名/风格/体量关键词）。命中则用 size/路径标签约束；整栋骨架再 FAWE/Litematica 粘贴后局部改。禁止无视参考库直接「火柴盒」。
4. **体积宁小勿大**；禁止未确认占地的整片 `air` / `grass_block`。
5. 误操作：先 `write_undo`（可多次）；不够或历史已丢 → 停手 → 停服 → 从 `D:\Minecraft\服务器\26.2.zip` 抽 region 还原。

## 推荐建造顺序

1. AABB / 标高 → 地基与地板  
2. 外墙与门窗洞（洞口尺寸匹配门扇）  
3. 梁柱网与屋顶/外挑（有梁必有柱；檐口贴墙或托砖）  
4. 灯与立面细节（吊灯间距 3–7，优先偶间距）  
5. 室内分区与家具（清家具勿拆柱梁与幕墙）  
6. 收工自检（悬浮灯、椅子 facing 取反、门接室外地坪、员工门≥2 等）

后一步禁止覆盖前一步结构件。椅子楼梯：`facing` = 视觉朝桌方向的**反方向**。

## 构图原则（从上游 skill 保留）

- **负空间**：建筑之间的巷弄、缝、前广场比单体堆料更重要。
- **高度节奏**：街区要有高低差，避免整排等高火柴盒。
- **底层可读**：临街玻璃/雨棚/招牌/入口照明；背面可有管道、逃生梯、垃圾区等「脏细节」。
- **材质分层**：壳材 + 玻璃节奏 + 层间腰线；屋顶要有收边/天线/设备感，不要平切。
- **对照参考**：照片或投影 → 建骨架 → 抽查 `get_block` / 玩家反馈 → 再改，不要一次糊完不验收。

## 坐标速记

- +X 东 / −X 西；+Z 南 / −Z 北；Y 向上。
- 北墙、东墙外侧像素字易「镜像」：写字前先想观众站在哪一侧看。

## 明确不做

- 不装 mineflayer BuilderBot、不按上游 `nogui` / WSL 教程另开服。
- 不把上游 `build-city.js` 等脚本当默认可执行路径（除非用户要求且已改接 BuildBridge）。
- 不把 FAWE `//undo` 当成直写撤销。
