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
        "MCWWS_BUILD_TOKEN": "粘贴-config.yml-里的-token"
      }
    }
  }
}
```

改 token 后重启 Cursor MCP / 重开 Agent 会话。

## 工具一览

| 工具 | 作用 |
| --- | --- |
| `list_players` / `get_player_pos` / `get_block` | 查询 |
| `set_block` / `fill` / `set_blocks` | Bukkit 直写 |
| `fawe_pos` / `fawe_set` / `fawe_replace` | 选区与设置 |
| `fawe_copy` / `fawe_paste` / `fawe_undo` / `fawe_redo` | 剪贴板与撤销 |
| `fawe_schem_list` / `fawe_schem_load` / `fawe_schem_paste` | schematic |

仅监听 `127.0.0.1`，Bearer token 鉴权。管理端工具，不走网页商城 `:8002`。
