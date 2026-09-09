# MCWWS Build Bridge MCP

Cursor 通过本 MCP 调用本机 `MCWWS_BuildBridge` HTTP API，在 Paper 服上直写方块并执行 FAWE。

## 前置

1. 编译并加载插件：`tools/mcwws-build-bridge/build.ps1` → `plugins/MCWWS_BuildBridge-1.0.0.jar`（换 jar 需停服）
2. 开服后日志出现 `BuildBridge HTTP on 127.0.0.1:8765`
3. 从 `plugins/MCWWS_BuildBridge/config.yml` 复制 `http.token`（首次启动会自动生成）
4. 在本目录执行 `npm install`

FAWE 选区 / paste 到坐标：若控制台会话不稳定，在插件配置填写在线 OP：

```yaml
actor-player: YourName
```

## Cursor `mcp.json` 示例

路径按本机仓库调整：

```json
{
  "mcpServers": {
    "mcwws-build-bridge": {
      "command": "node",
      "args": [
        "D:/Minecraft/服务器/26.2/tools/mcwws-build-bridge-mcp/src/index.js"
      ],
      "env": {
        "MCWWS_BUILD_URL": "http://127.0.0.1:8765",
        "MCWWS_BUILD_TOKEN": "粘贴-config.yml-里的-token",
        "MCWWS_BUILD_INSECURE_TLS": "1"
      }
    }
  }
}
```

远端经樱花 HTTPS 时：`MCWWS_BUILD_URL` 用 `https://域名:端口`，并保留 `MCWWS_BUILD_INSECURE_TLS=1`（樱花自签证书，否则 Node 会报 `DEPTH_ZERO_SELF_SIGNED_CERT`）。

改 token 后重启 Cursor MCP / 重开 Agent 会话。

## 工具一览

| 工具 | 作用 |
| --- | --- |
| `list_players` / `get_player_pos` / `get_block` | 查询 |
| `set_block` / `fill` / `set_blocks` | Bukkit 直写（自动记入历史） |
| `write_undo` / `write_redo` / `write_history` | 直写历史撤销/重做/查看（与 FAWE `//undo` 独立） |
| `fawe_pos` / `fawe_set` / `fawe_replace` | 选区与设置 |
| `fawe_copy` / `fawe_paste` / `fawe_undo` / `fawe_redo` | 剪贴板与撤销 |
| `fawe_schem_list` / `fawe_schem_load` / `fawe_schem_paste` | schematic |
| `schem_index_status` / `schem_search` / `schem_info` / `schem_reindex` | 本地 Litematica 参考库检索（建建筑前先搜） |

## Litematica 参考库索引

默认扫描：`D:/Minecraft/游戏主体/.minecraft/schematics`（可用环境变量 `MCWWS_SCHEM_ROOT` 覆盖）。

首次或新增大量投影后，在本目录建索引（约 1.7 万文件，完整解析可能数分钟～十几分钟）：

```text
npm run schem:index
```

只要文件名/文件夹标签、不要 NBT 元数据时：

```text
npm run schem:index:quick
```

索引落在 `data/schem-index.json`（已 gitignore）。Agent 建造前应调用 `schem_search`（如「现代快餐 玻璃幕墙」），根据命中路径做材质/体量参考；需要整栋粘贴时再走 FAWE schem（把文件拷到 FAWE schematics 目录）或人工 Litematica。

`mcp.json` 可选：

```json
"MCWWS_SCHEM_ROOT": "D:/Minecraft/游戏主体/.minecraft/schematics"
```

仅监听 `127.0.0.1`，Bearer token 鉴权。管理端工具，不走网页商城 `:8002`。
