# 插件待更新目录

本目录用于存放**已检测到新版本**、从 Modrinth/Hangar/GitHub/GeyserMC 下载的 jar，**不会自动替换** `plugins/` 里的旧文件。

## 本次扫描结果（2026-08-02，已补全）

- 扫描：**138** 个 jar
- 已下载到本目录：**39** 个 jar（见下方列表）
- 完整报告：`update-report.txt`、`update-report.json`

### 核心基础设施（本次补全）

| 插件 | 当前 → 新版 | 来源 |
|------|-------------|------|
| LuckPerms | 5.4.158 → 5.5.53 | Modrinth |
| WorldGuard | 7.0.13 → 7.0.17 | Modrinth |
| FastAsyncWorldEdit | 2.15.2 → 2.15.3 | Modrinth |
| PlaceholderAPI | 2.11.7 → 2.12.3 | Modrinth |
| BKCommonLib | 1.21.11-v1 → 2.0.1 | Modrinth |
| Train_Carts | 1.21.11-v2 → 2.0.0-1720 | Modrinth |
| VeinMiner | 2.2.6 → 2.11.2+1.21.11 | Modrinth |

### EssentialsX 全套 2.22.0（需配套替换）

| 文件 | 说明 |
|------|------|
| EssentialsX-2.22.0.jar | 核心 |
| EssentialsXChat-2.22.0.jar | 聊天 |
| EssentialsXSpawn-2.22.0.jar | 出生点 |
| EssentialsXGeoIP-2.22.0.jar | GeoIP（GitHub Release） |

### 基岩跨平台（SNAPSHOT → 正式版，请人工确认）

| 插件 | 当前 → 新版 | 说明 |
|------|-------------|------|
| Geyser-Spigot | 2.9.2-SNAPSHOT → 2.11.0-b1205 | Modrinth |
| floodgate | 2.2.4-SNAPSHOT → 2.2.5-b138 | GeyserMC API（官方最新 spigot） |

### 其他已下载更新

ajLeaderboards、BlueMap、CommandAPI（**12.0.0 大版本**）、CommandTimer、DecentHolograms、GriefPrevention、GSit、HeadDB（**6.0.0-rc.2**）、InventoryRollbackPlus、ItemEdit、NBTAPI、PlayerParticles、PluginPortal、ServerVariables、Skript、skript-placeholders、UltimateShop、WorldEditSUI 等。

### 仍需手动处理

- **Citizens**（2.0.41-SNAPSHOT）— SpigotMC 构建站，无稳定 Modrinth 源
- 全部 **Slimefun 附属**（Build xxx git）
- **Residence**、**DeluxeMenus**、**ProtocolLib**、**CMILib** 等 Spigot/自建源
- **MCWWS_*** 自研插件（跳过）
- 部分 Modrinth 误匹配项（ColorGradient→Timer 等）— 升级前请核对 `plugin.yml` 名称

## 如何再次扫描

```powershell
# 主扫描（Modrinth + Hangar versions API）
python tools/mcwws-plugin-updates/check_and_download.py          # 仅报告
python tools/mcwws-plugin-updates/check_and_download.py --download

# 补下 GeoIP / floodgate 等无 Modrinth 源的插件
python tools/mcwws-plugin-updates/download_supplement.py
```

可在 `tools/mcwws-plugin-updates/plugin-sources.json` 里追加 Modrinth/Hangar 源。

## 替换步骤

1. 停服或 `/stop`
2. 备份 `plugins/` 对应旧 jar
3. 将本目录中新 jar 复制到 `plugins/`
   - EssentialsX 四个 jar **必须同版本一起换**
   - Geyser + floodgate 建议一起升级并测试基岩端登录
   - Train_Carts 2.0.0 与 BKCommonLib 2.0.1 需配套
4. 启动并查看控制台报错
