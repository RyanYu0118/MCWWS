# 流浪世界服务 · HarmonyOS 7

鸿蒙原生 Stage 应用，使用 ArkTS、ArkUI 和 ArkWeb。工程目标为 HarmonyOS 7.0（API 26.0.0），包名为 `work.mcwws.harmony`。

- 主页使用随应用打包的夜景截图，启动后即可显示。底部使用系统的悬浮 TabBar 和沉浸式材质，保留主页、商店、地图、管理、账户五个入口。
- 商店、地图、管理和账户在 ArkWeb 中打开现有 HTTPS 页面，继续使用服务器上的数据及登录权限。首次安装需在应用内登录。地图页需要能访问主站与 BlueMap 子域，已浏览瓦片的离线可用性需要在鸿蒙真机验证。
- 在地图中横向拖动时，手势交给地图。在网页页签中按系统返回键，会先返回该网页的历史记录，页签没有历史记录时回到主页。

## 用 DevEco Studio 装到手机

1. 从华为官网下载并安装带 HarmonyOS 26.0.0 Release SDK 的 Windows 版 DevEco Studio，例如 26.0.0 Release (26.0.0.865)。用 **File → Open** 打开本目录，等待项目同步。[官方工具下载](https://developer.huawei.com/consumer/cn/deveco-studio/resources/) · [API 26.0.0 配套版本](https://developer.huawei.com/consumer/cn/doc/harmonyos-releases/overview-2600/)
2. 打开手机的 **设置 → 关于手机**，连续点七次 **软件版本／版本号**，按提示启用开发者模式。进入 **设置 → 系统（或系统和更新）→ 开发者选项** 打开 **USB 调试**。用能传输数据的 USB 线连接，解锁手机并确认调试授权。[华为真机安装说明](https://developer.huawei.com/consumer/cn/doc/harmonyos-faqs/app-debugging-faq-0005/)
3. 在 DevEco Studio 顶部设备列表选中手机。打开 **File → Project Structure → Project → Signing Configs**，选中 `default` 产品的 **Automatically generate signature**，按提示登录华为开发者账号并完成签名。若提示添加测试设备，根据自动签名向导操作。签名配置和证书保存在本机，请勿提交到 Git。[华为自动签名说明](https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/ide-signing-auto/)
4. 选择 `entry` 并点击绿色 **Run ▶**。DevEco Studio 会编译、签名并将调试 HAP 安装到已授权的手机。

## 用 hdc 安装已签名的 HAP

完成签名和构建后，使用 SDK 的 HarmonyOS Device Connector（`hdc`）。下面两条命令在工程根目录运行。若构建窗口显示了其他文件名或目录，填入实际生成的签名包路径。

```powershell
hdc list targets
hdc install ".\entry\build\default\outputs\default\entry-default-signed.hap"
```

- 设备列表显示 `[Empty]` 时，确认数据线、USB 调试和手机端的授权弹窗。
- 签名校验失败时，检查调试签名和目标设备授权。把工程发到另一台电脑时，可用对方的华为开发者账号为其手机生成签名，再点 **Run**。

## 真机验收

检查主页离线启动、悬浮栏与系统手势区、地图拖动与 2D／3D、商店购物车、账户登录后商店和管理的权限、错误页重试以及应用退出重开后的地图缓存。
