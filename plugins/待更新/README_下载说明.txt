待更新插件（26.2）下载结果
============================

已下载（可替换旧版）：
- Citizens-2.0.43-b4231.jar          ← 替换 Citizens-*.jar
- Denizen-1.3.3-b7299-DEV.jar       ← 替换 Denizen-*.jar（含 26.2 模块）
- Sentinel-2.9.4-SNAPSHOT-b534.jar  ← 替换 Sentinel-*.jar
- Depenizen-2.1.1-b885.jar          ← 替换 Depenizen-*.jar
- BKCommonLib-2.0.2-SNAPSHOT-2029.jar
- TrainCarts-2.0.1-SNAPSHOT-1734.jar
- TCCoasters-2.0.1-SNAPSHOT-408.jar
- fake-player-plugin-2.0.3.jar      ← 原 fakeplayer-0.3.19 无官方 26.2，此为兼容替代（FPP）

客户端模组（不要放进 plugins）：
- 客户端模组_非服务端插件/SmoothCoasters-Fabric-26.2-v2-SNAPSHOT.jar
  原 plugins 里的 SmoothCoasters-1.21.11-v1.jar 不是服务端插件，应删除。

未下载 / 需你自行处理：
- AxiomPaper：你已自行升级
- denizen-reflect / Webizen：随 Denizen 升级后再测；若仍炸再单独升级
- 原 tanyaofei fakeplayer：暂无 26.2 官方包

替换步骤：
1. 停服
2. 把 plugins 里对应旧 jar 移到 plugins/旧版备份/
3. 把本目录新 jar 复制到 plugins/
4. 开服检查 logs/latest.log
