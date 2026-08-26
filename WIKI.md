# 流浪世界（MCWWS）玩家指南

> 本文面向**进服游玩的你**：用现实世界的学科眼光，理解这片大陆上的规则与生活方式。  
> 本服是 Paper **26.2** 的**始终生存**服务器，对外简称 **MCWWS**，制作组 **RYAN STUDIO**。  
> 公开站点 [RYAN-STUDIO](https://github.com/RyanYu0118/RYAN-STUDIO) 会据此发布；若游戏内体验与本文不一致，以游戏内提示为准。

---

## 阅读说明

- **两大篇**：**自然科学**描述空间、物质、生命及其规律；**社会科学**描述交换、制度、人与人的协作。
- **自然科学学科**（按篇内顺序）：地理学、物理学、生物学、化学、**魔法学**、**合成学**、**计算机学**、**建筑学**、承载学。
- **层级**：篇 → 学科 → 主题 → 正文中的具体玩法。插件名、指令名只出现在需要操作的地方，不作为章节标题。
- **速览**在篇首；全服插件与数据包逐项说明见**附录 E、F**。

---

## 速览

| 自然科学 · 你摸得到的世界 | 社会科学 · 你参与其中的秩序 |
| ------------------------- | --------------------------- |
| 始终生存，但可用工具大规模改造地貌与建筑，改造按市场计价 | 买价与卖价联动涨跌，商店、网页、建造共用一套物价 |
| 浏览器可览全图、画路、做规划 | 网页账本记录每一笔零钱，精确到毫秒 |
| 指南传送按距离计费，跨维度另加收 | 领地划界，中文旗标与提示 |
| 光源须手持才能拆改；砍树可连根放倒并补种；潜行连锁采矿最多 64 格 | 与 NPC 对话完成新手引导；L 键仅「流浪世界」一页；成就用 `/aach list` |
| 大规模改造分「指令批量」与「可视化编辑」两种方式，规则一致 | 撤销退 95%；同次编辑内搬运同种方块只收劳务 |
| 粘液科技指南首次进服赠送；配方书与自定义合成 GUI 全员可浏览 | 每日签到 `/signin gui`；服务器告示 `/news`；基岩版经 Geyser 同端口进服 |
| 护符、炼金注入与末地奇术等魔法向内容分散在 Slimefun 附属中 | 商店附魔分类与动态物价联动；领地可限制负面药水 PvP 效果 |

---

# 一、自然科学

> 空间、物质、生命，以及它们在本服中的运行方式。

---

## 1. 地理学

研究**人在哪里、如何认识空间、如何移动**。

### 1.1 空间的性质

这是一张**始终生存**的地图：地貌与建筑可以被大规模改造，但**每一格**都纳入经济计量——放置付钱、拆除回收、搬运另计。  
普通生存交互（村民、展示框、盔甲架、门）优先于建造辅助界面；改造工具不会默认抢走你的右键。

### 1.2 测绘与规划

在浏览器打开本服三维地图，可以：

- 自由漫游俯瞰
- 绘制道路、区域，标注路名与建筑名
- 折线、箭头、套索、重心与轴向调整
- 导入 Litematica 材料列表做**规划**（不在网页上直接落地建造）
- 切换日夜与图层；远处按 **10000 格**分块，避免精度问题

地图上的商业网点标注与游戏内、网页侧的物价呼应。

> 网页侧「投影粘贴 / 材料清单一键落地」已废止；要在世界里实装，请在游戏内用可视化编辑工具粘贴原理图，并遵守生存改造计费规则（见**建筑学 · 物质的改造**）。

### 1.4 地貌与维度（数据包）

主世界 **`world`** 叠加以下世界生成数据包（详见附录 F）：

| 数据包 | 作用 |
| ------ | ---- |
| Terralith 26.2 v2.6.4 | 主世界生物群系与地貌大幅扩展 |
| Terratonic 3.0.27 | 与 Terralith 配套的地形抬升与构造 |
| Amplified Nether 1.2.15 | 下界地形放大与景观 |
| Nullscape 26.2 v1.2.20 | 末地维度景观与结构 |
| Structory 1.3.7 / Structory Towers 1.0.17 | ambient 结构与塔楼点缀 |

**`dimensionalhome`** 为额外家园维度；**`world_nether`**、**`world_the_end`** 与主世界共享上述下界/末地包。  
**One_day** 插件按和风天气 API 将主世界天气与真实城市同步（配置于 `plugins/One_day/`）。

### 1.3 位移与距离

指南菜单中的**定点传送**与**传送至玩家**共用一套距离律：

```
16/1225 × |d − 100| − 16/1225 × |d − 5000| + 64
```

- `d`：起点与终点的三维直线距离（跨世界不按下界 1:8 换算）
- **同维度 100 格内**：免费
- **5000 格外**：距离费封顶 **¥128**
- **不在同一世界**：另加 **¥64** 维度转接费

传送至玩家：点选头颅后约 **2 tick** 弹出列表，弹出前勿重复打开指南。由**发起者**付费；选人前金额未定，零钱不足则取消。

指南首页另有三类**标记 / 回溯**（全体玩家默认可用，经菜单扣费）：

| 功能 | 操作 | 费用 |
| ---- | ---- | ---- |
| 个人标记点 | 左/右键设 1/2 号；Shift+左/右键传送 | 设置或传送各 **¥64**/次 |
| 公共标记点 | 左键设置；Shift+左键传送 | 设置 **¥128**/次，传送 **¥48**/次 |
| 返回上一记录点 | 点选返回 Essentials 上一位置 | **¥64**/次 |

个人标记命名为「玩家名1/2」；公共标记与床重生点无关。

打开指南：右键服务器指南针，或在聊天栏只输入斜杠并发送（无子命令）：

```text
/
```

---

## 2. 物理学

研究**光、力与能量**在本服中的特殊规律。

### 2.1 光与照明

生存 / 冒险模式下：

- **左键**拆除光源：须主手或副手**手持光源方块**，否则敲不掉、也不掉落
- **右键**已放置的光源：须手持光源，调节亮度 **0–15**
- **手持光源对空气右键**：选择放置亮度（默认 15）

世界中的光源图标随真实亮度变化。无权限的破坏仍受领地规则拦截。

### 2.2 采矿力学

**连锁采矿（VeinMiner）：** 潜行状态下破坏矿石或同类方块，一次最多连锁 **64** 格，每次收费 **¥16**（插件 `Cost: 16.0`）。与连根砍树（生物学）独立，互不干扰。

**飞行耗能：** 生存飞行按段计费并记入网页零钱账本（`FlyWithFood` + `MCWWS_EconomyLedger`）；Essentials 自带飞行扣费由账本插件去重，避免双扣。

---

## 3. 生物学

研究**生命体与环境的取用与再生**。

### 3.1 植被采伐

持斧砍树可一次放倒整棵，单次最多 **150** 根原木，耐久按数量扣除；默认整树旋转倒地动画。树干基座自动补种树苗，树叶落地 **1%** 概率补种。全员默认可用；潜行不关闭。倒下方块砸中你造成 **1** 点伤害。

**ExoticGarden 果树**（橙子、椰子、荔枝等 Slimefun 树苗长成的树）同样支持连根砍；树桩会自动补种对应的**果树苗**（不是原版树苗），树叶**不掉落树叶方块**，而是按 **5%**/片概率额外掉落果树苗（可在 `MCWWS_UltimateTimberFix` 配置中调整）。果实仍建议右键采摘。

### 3.2 养殖与作物（粘液科技扩展）

以下 Slimefun 附属侧重**生命资源的培育与取用**（配方见合成学 · 粘液科技指南）：

| 附属 | 你能做什么 |
| ---- | ---------- |
| Cultivation | 作物经验与图鉴式种植玩法 |
| ExoticGarden | 新作物与植物资源 |
| GeneticChickengineering | 鸡的品种改良与自动化养殖 |
| SlimyBees | 养蜂与蜂产品 |
| Gastronomicon | 烹饪与食物加工链 |
| MobCapturer | 将生物捕获进罐子（配合 SoulJars 等） |
| ElectricSpawners | 电力刷怪笼 |
| HotbarPets | 快捷栏宠物伙伴 |
| FlowerPower | 花卉相关资源 |

原版农业、钓鱼、驯养与上述机器可并存；辐射区（化学）对未防护玩家仍生效。

---

## 4. 化学

研究**物质的转化、反应与危险形态**——本服以粘液科技（Slimefun）为核心，并叠加炼金、酿造与战斗化学类附属。

### 4.1 辐射与防护

- 辐射系统**开启**（`enable-radiation: true`），进入辐射区有 **15 tick** 宽限期后持续扣血
- 穿着放射性护甲或持有辐射相关物品时，护甲效果按配置周期刷新
- 核反应堆、辐射矿石等高风险设施须按指南搭建屏蔽层；**WorldEditSlimefun** 允许在选区改造时识别 Slimefun 特殊方块

### 4.2 工艺流程与酿造

| 附属 / 插件 | 侧重 |
| ----------- | ---- |
| Slimefun 本体 | 电力冶炼、矿石洗炼、粉尘/宝石加工等**工艺流程**（非工作台配方，见合成学） |
| UltimateFoods | 高级营养向食品（亦见生物学 · 养殖与作物） |

酿造台、炼药锅与商店「酿造」分类仍按原版与 UltimateShop 定价流通。**附魔、护符、炼金注入与奇术物品**见**魔法学**。

### 4.3 能源化工

**EcoPower**、**DynaTech**、**BedrockTechnology** 等提供太阳能、地热、燃油与高级发电机；电力网络与物流接口见**计算机学**。

---

## 5. 魔法学

研究**超自然力量如何附着于物品、生物与空间**——附魔、炼金注入、护符、灵魂容器与末地奇术等。

### 5.1 附魔与咒语

- **UltimateShop**「附魔」分类：按效果与等级定价，与动态市场联动
- **Slimefun** 魔法工作台、自动魔法工作台：高级魔法物品的合成入口（具体配方见**合成学**）
- **InfinityExpansion** 高级铁砧 / 无尽套装：附魔等级上限可在配置中调整（`plugins/InfinityExpansion/config.yml`）
- **Supreme**「magic」系列武器与防具附魔（`plugins/Supreme/config.yml` · `supreme-enchant`）
- 巫师护符、魔法尘、魔法奇点等 Slimefun 本体物品按指南分类解锁

### 5.2 炼金、注入与演变

**AlchimiaVitae**（生命炼金）本服启用注入、演变与仁慈酿造，例如：

| 类型 | 示例 |
| ---- | ---- |
| 注入（Infusion） | 毁灭/幻影暴击、自瞄、强力、挥发、治疗、自动补种、图腾电池、击退等 |
| 演变（Transmutation） | 强化合金、硬化金属、钢锭、大马士革钢、压缩碳等 |
| 仁慈酿造 | 可配置时长与等级的抗性提升、急迫等药水 |

**Alchema** 提供炼金台与转化配方；**PotionExpansion** 扩展原版药水效果池。

### 5.3 护符、灵魂与召唤

- **Slimefun 护符（Talismans）**： magician / ender magician 等；触发时以 ActionBar 提示（`use-actionbar: true`）
- **SoulJars** + **MobCapturer**：捕获生物灵魂入库，用于特定配方或玩法
- **SlimefunLuckyBlocks**：幸运 / 不幸药水方块（配置可单独开关）
- **FlowerPower** 等花卉系道具偏魔法辅助与资源产出

### 5.4 奇术与不稳定力量

**TranscEndence**（超恒 / 末地奇术）：

- 首次进服可获专属指南
- **不稳定锭** 默认不可丢弃；相关死亡有专属提示文案
- **Daxi 核心** 提供力量、吸收、坚韧、饱和、再生等超能力（等级见 `plugins/TranscEndence/config.yml`）
- 极化器亲和概率 **40%**；ZOT 需 **1000** 电荷

**DracFun**、**Bump**（鉴定 / 增幅）、**SlimefunWarfare**、**FNAmplifications** 等偏战斗魔法与装备成长，成品多在商店「服内特有」或指南分类中流通。

**领地规则：** **Residence** 可按 flag 限制负面药水在 PvP 场景生效（见 `NegativePotionEffects` 配置）。

---

## 6. 合成学

研究**如何把原材料组合成可用物品**——工作台配方、粘液科技合成台、自定义 GUI 配方与本服合成规则。

### 6.1 配方查阅

| 途径 | 说明 |
| ---- | ---- |
| 原版 / 粘液科技指南 | 首次进服自动获得 Slimefun 指南（`receive-on-first-join: true`）；指南内**展示原版合成** |
| CustomCrafting | 图形化自定义配方浏览器；普通玩家默认可浏览全部分类 |
| SFCalc | 游戏内 `/sfcalc` 类计算器（Slimefun 配方反查，见附录 E） |
| UltimateShop | 店内物品即物价与流通入口，不替代配方书 |

### 6.2 手持工具与放置规则

Slimefun **手持类物品**（便携式工作台/垃圾桶、各类背包、GPS 标记器、卷尺等，以及所有 `PORTABLE_*` 物品）只能手持右键使用，**不能放置**；放下再拆会失去粘液科技功能，变成普通方块或头颅。

以下两类**均可正常放置**，不受拦截：

- **普通机器**：货运管理器、太阳能板、电力冶炼炉、外卖分配器、外卖柜等
- **多方块机器**：增强型合成台、磨石、矿石粉碎机、压缩机、装甲台、魔法工作台等（魔法向机器亦见**魔法学**）

Skript `portable_crafter_place.sk` 在服务端强制拦截违规放置。

### 6.3 大型合成附属（选读）

| 附属 | 内容概要 |
| ---- | -------- |
| Supreme / InfinityExpansion / LiteXpansion | 终局材料、奇异物质、UU 物质等高层合成链 |
| FluffyMachines / FoxyMachines / ExtraTools | 工具与中型机器配方 |
| SlimeTinker | 模块化工具锻造与词条 |
| SaneCrafting | 配方冲突修复与平衡补丁 |
| Cakecraft / Bump / TranscEndence 等 | 主题配方包，按指南分类解锁 |

研究（Research）默认用经验解锁；创造模式玩家可免费解锁全部研究。

---

## 7. 计算机学

研究**信息、物流与自动执行**——粘液科技的货运网络、GPS、可编程机器，以及本服脚本与 NPC 自动化。

### 7.1 电力与物流

| 系统 | 要点 |
| ---- | ---- |
| 能源网络 | 发电机 → 蓄电器 → 机器；不同附属机器共享 Slimefun 电力 API |
| 货运网络 | 输入/输出节点、高级节点、货运管理器；网络最大 **200** 节点，可视化器默认开启 |
| GPS | 传送点与地理标记；本服上限 **21** 个 GPS 路点 |
| 自动化矿机 / 刷石机 | 编程矿工、电动洗矿、各种 Generator；按格耗电与产物进入货运 |

**WorldEditSlimefun** 与领地插件会保护已登记的 Slimefun 方块，避免误改选区时破坏机器 NBT。

### 7.2 计算与脚本层（服内）

| 组件 | 作用 |
| ---- | ---- |
| Skript `mcwws/` | 动态物价、传送计价、光源规则、账本队列等**服务器逻辑** |
| Denizen + Citizens + Sentinel | NPC 对话、新手任务、守卫 AI |
| ScriptBlockPlus / ScriptEntityPlus | 踩方块 / 点实体触发脚本（中文配置） |
| BetonQuest | 新手包 `mcwws_newbie` 任务状态机 |
| CommandTimer | 定时执行控制台指令 |
| ServerVariables | 跨脚本的全局变量 |

玩家日常只需与 NPC 和指南菜单交互；上表供想深入 Redstone 式自动化的玩家理解「谁在后台算账」。

### 7.3 终端与界面

- **ScreenInMC**：游戏内嵌网页屏幕（需客户端资源，配置见 `plugins/ScreenInMC/`）
- **BlueMap + MCWWS WebHost**：浏览器端地图与商城、账本（见地理学 · 测绘）
- **CommandPrompterPaper**：聊天栏分步提示长命令，减少记指令负担

---

## 8. 建筑学

研究**人造空间的形式、尺度与交通**；本服允许在生存状态下进行**创造级规模**改造，并辅以曲线建造、轨道交通与装饰工具。

### 8.1 物质的改造（生存计费）

主要有两条路径，**计费通则相同**（详见社会科学 · 经济学 · 改造与市场的关系）：

| 路径 | 适合做什么 | 你如何开始 |
| ---- | ---------- | ---------- |
| 指令批量改造 | 选区填充、替换、剪贴板、几何体、附近工具等 | 熟悉 `//set`、`//replace`、`//paste` 等指令的玩家 |
| 可视化编辑 | 笔刷、原理图粘贴、实体摆放、生物群系画笔等 | 安装配套客户端模组后，在生存中打开 Editor |

**共通限制（节选）：**

- 单次扫描超过 **50 万格**拒绝执行
- 笔刷、填洞工具、平滑 / 变形 / 再生、改生物群系等**不在生存放行范围**
- 选区内的特殊机器与不可破坏方块保留，不计费
- 聊天前缀分别为 **`[创世神]`** 与 **`[Axiom]`**，扣费提示会合并显示

#### 指令批量改造 · 要点

会改方块且纳入计费的指令包括：`set`、`replace`、`stack`、`walls`、`overlay`/`lay`、`faces`/`outline`、`hollow`、`center`/`middle`、`line`、`curve`、`move`/`mv`、`fall`、`naturalize`、`forest`、`flora`、`cut`、`paste`/`p`/`pa`、`place`、`cyl`/`hcyl`、`sphere`/`hsphere`、`pyramid`/`hpyramid`、`cone`，以及 `replacenear`、`removenear`、`removeabove`、`removebelow`、`drain`、`extinguish`、`snow`、`thaw`、`green`、`fixlava`、`fixwater` 等。

`//re` 是 replace，不是重做；重做请用 `//redo`。`//forest` / `//flora` 按预扫描扣费，树冠可能与实际略有出入。

**单次净额：**

```
净额 = 材料费 + 劳务费 − 拆除回收
```

| 项目 | 规则 |
| ---- | ---- |
| 劳务费·放置 | 每格 **0.5** |
| 劳务费·拆除 | 每格 **1.0** |
| 材料费 | 新放置 × 市场买价 |
| 拆除回收 | 拆下 × 市场卖价 × **100%** |
| 搬运 | 同种方块对冲：只收劳务，不走买卖 |

`//move` 若指定留下方块则视为复制；`//stack` 向空气格复制仍收材料费。改选区后立刻操作会重新扫描，以聊天提示为准；连续两次 `//set` 各扣一次。

**撤销与重做：** `//undo` 逐笔退 **95%**；`//redo` 按原额再扣；撤销后新建造会清空 redo 栈；拆除净收入撤销时若余额不足则取消。

#### 可视化编辑 · 要点

服务端**始终是生存**；Editor 在客户端配合打开。需安装：

| 端 | 当前版本 |
| -- | -------- |
| 服务端 AxiomPaper | 5.0.4（MC 26.2） |
| 服务端生存扣费组件 | 1.1.7 |
| 客户端 Axiom | 5.5.0（MC 26.2） |
| 客户端生存配套模组 | **1.2.5+** |

流程摘要：开菜单时本地旁观；建造时 `E` 仍是生存背包、挖掘按生存进度；退出后位置与模式恢复，**编辑界面调的飞行速度会保留**（需客户端 1.2.5+）。进入 Editor 不会自动开飞。

额外计费：实体生成 **20** / 删除 **5** / 调整 **2**；生物群系画笔每格 **0.02**。生存中不可改世界时间、属性或切创造。受保护格写入后还原；超 **4096** 格则整包取消。超大笔刷拆包后，**仅包内**搬运可对冲；撤销按「放置/拆除对调 + 10 分钟内」配对。

**易误触：** 附近展示实体可能显示可拖动小方块，会抢走村民、展示框等右键——**默认全员关闭**，须单独授权（见政治学 · 准入）。生存中建议关闭 Fast Place；推荐 Slimefun 材质包以统一界面网格。

卡住时：

```text
/axiomrestore
```

```text
/axiomcheck
```

### 8.2 曲线与曲面

**CurveBuilding** 提供曲线/曲面选区与放置辅助，适合道路、拱券、穹顶等自由形态；与 WorldEdit 选区配合使用，改造方块仍走**建筑学 · 物质的改造**计费。

### 8.3 轨道交通

**TrainCarts** + **TCCoasters** 实现自定义矿车列车与过山车：

- 使用服务端 `server.properties` 中的资源包（`resourcePack: server`）
- 车厢间距、动力铁轨加速、最大速度等见 `plugins/Train_Carts/config.yml`
- 领地内铺设轨道须自有放置权限；高速列车请避开他人未授权区域

### 8.4 装饰与展示

| 工具 | 用途 |
| ---- | ---- |
| ColoredAnvils / AnvilPanel | 染色铁砧、自定义铁砧 GUI 面板 |
| HeadDB | 自定义头颅库，建筑软装与招牌 |
| DecentHolograms | 悬浮文字说明牌 |
| PlayerParticles | 个人粒子特效 |
| ColorGradient | 渐变文字/物品名 |
| GSit | 坐、躺、趴（`/gsit` 系列，潜行可起身） |
| CreeperConfettiPro | 苦力怕死亡彩纸特效（纯视觉） |

**RandomBlockPlacement** 微调方块朝向随机性，偏景观自然感；**WorldEditSUI** 为选区可视化叠加层。

---

## 9. 承载学（物质的空间占有）

研究**物品如何随身存放**——本服对默认行囊行为做了调整。

### 9.1 随身行囊与拾取策略

- **不发放**头颅形态的扩展背包物品；已有的会被收回，不可放置、不可捡起
- **关闭**自动拾取进扩展背包：地上物品不会自动转入

打开随身行囊：指南菜单左键，或

```text
/bags open
```

**外卖分配器 + 外卖柜（Slimefun / RSC）：** 两台机器配合，**一码一柜**。

- **外卖分配器**（增强型合成台：铁锭 + 箱子）：**Cabinet 玩家头颅**外观（[Minecraft-Heads #3614](https://minecraft-heads.com/custom-heads/head/3614-cabinet)）。放置后由**管理员**聊天输入**大编号**（网页收货地址，最长 24 字）。**无存储**。右键界面可输入一次性**取件码**；成功后提示分配到的**小编号**，对应木桶**开盖**。改大编号仅管理员（OP 或 `mcwws.delivery.admin`）。已放置的旧版一体头颅机自动当作分配器。
- **外卖柜**（增强型合成台：铁锭 + 木桶）：**木桶**外观。由**管理员**设置与分配器相同的**大编号**及该址内唯一的**小编号**。默认**上锁**打不开；分配器输码后该柜开盖，**直到柜内取空**都可反复打开。取空后关盖上锁，可再次接收网页订单。柜内还有东西时**不可再分配**。**潜行右键**不打开界面，可在已有柜上继续叠放。解锁状态同时写在柜体方块数据上，避免网页误回写注册表后「盖已开却仍上锁」。开/关柜用原版木桶音效，上锁/解锁用容器上锁音效，操作成功/失败另有提示音。改编号仅管理员。
- **NFC 修改器**（增强型合成台）：**大编号**用试炼钥匙外观，右键聊天写入后潜行右键分配器/柜。**小编号**用不祥试炼钥匙外观，默认从 **1** 起，每对柜使用一次自动 **+1**；右键可把下次起始值改成任意数字 n。**仅管理员**可用；柜被订单占用时不可改号。

网页下单须选择已有**分配器大编号**，生成 6 位取件码（下单成功弹窗与购物车待领取列表均可**选中或一键复制**）。整单只进入该址下一台**空闲木桶柜**（空且未占用）。无空闲柜则订单失败并重试。漏斗与货运节点不能抽放。拆除时内容物掉落并注销编号。未编号的分配器不会出现在网页地址列表。旧的无地址订单仍发到随身行囊。手工放入的物品在柜已解锁时也可存取。

### 9.2 粘液科技储物

| 附属 | 说明 |
| ---- | ---- |
| DyedBackpacks | 染色背包，可升级容量 |
| ColoredEnderChests | 染色末影箱 |
| PrivateStorage | 私人储物方块 |
| SoulJars | 灵魂罐存储 |
| InfinityExpansion | 高级存储单元与虚空柜 |
| BackpackPlus | 额外背包机制（与 BetterBags 政策并存，见 8.1） |

货运节点**不能**抽放外卖柜内物品；柜体与 Slimefun 机器一样受领地保护。

---

# 二、社会科学

> 交换、制度、身份与协作。

---

## 1. 经济学

研究**稀缺、价格、流通与记录**。

### 1.1 市场规律

- **买入价**与**卖出价**共用同一倍率，防止单边套利
- 回收价不高于买入价；若算出更高，压至买入价的 **99%**
- 价格两位小数；考古类 **+10%**、木材类 **-10%** 叠在动态倍率上
- 所有交易渠道（店内、网页、改造计费）读取**同一份**物价

涨跌受**虚拟库存**与**近期买卖压力**影响，并随时间缓慢回落。买入减库存、加压；卖出相反。

### 1.2 改造与市场的关系

| 操作 | 钱与库存 |
| ---- | -------- |
| 放置 | 按买价扣材料，减库存 |
| 拆除 | 按卖价 × 100% 回收，加库存 |
| 搬运 | 同种方块对冲，**不改库存** |

可视化编辑拆成多包时，仅**同一包内**搬运可对冲（见**建筑学 · 物质的改造**）。

### 1.3 记账与明细

飞行、贸易、传送、改造、连锁采矿等变动记入**网页零钱账本**：

- 独立页签，余额约每秒刷新，时间戳到**毫秒**
- 飞行按段合并：中断不超过约 **1 分钟**仍同段；落地超 **1 分钟**再飞才新开一行

### 1.4 交换的渠道

#### 店内贸易

- 分类对齐 **26.2 创造物品栏**；同物可出现在多类，同价
- 另设酿造、附魔（按效果/等级）、每日特供、服内特有物品等分类
- 刷怪蛋与管理员方块不流通

**文字搜索（必看）：** 点放大镜 → 点漏斗 → 菜单关闭 → 在聊天栏输入中文名（如「橡木」）→ 结果页重开。筛选槽放物品则无需打字。

```text
/shop searchgui
```

**快捷键**（以键位名为准，可在控制设置改绑定）：左/右键买卖 1 个；Shift+左键选购买量；Shift+右键选出售量；副手交换键收藏；丢弃键从本地仓库取回（最多 64，不扣钱）；Ctrl+丢弃键切换该物品是否自动吸取进仓库。

购买 **`ultradepository.*` 时效权限**后，符合条件的拾取/掉落进入**本地统一仓库**；聊天提示可点「返回至背包」，空间不足则整单留存；返回后 **1 分钟内**该物不再自动入库。商店内对该商品按 **Ctrl+丢弃键** 可持久关闭或重新开启自动吸取。

#### 网页贸易

- 购物车可手输数量，单件上限 **10000**
- 下单须选择或填写**收货地址**（外卖分配器大编号，网页支持搜索下拉），并获得一次性**取件码**；整单只进入该址一台空闲木桶柜
- 取件：下单游戏账号到**外卖分配器**输入**自己的**取件码，查看小编号；对应木桶**开盖**后仅该玩家可开，直到取空才关盖上锁；取空后网页订单变为**已取件**，不再显示在待取列表
- 无地址的旧订单仍在上线后写入随身行囊
- 记入网页日志与零钱明细；可嵌入公开 Wiki 页浏览

网页侧建造下单已废止；实装请回游戏内可视化编辑。

### 1.5 银行与零钱

- **EssentialsX**：主钱包，货币符号 **¥**，初始 **¥20**，上限 **¥10T**（Vault 桥接全服经济）
- **BankPlus**：独立银行账户与利息/转账（与主钱包并行，GUI 见指南或 `/bank`）
- **ajLeaderboards**：余额、统计等排行榜展示（常与全息/菜单联动）

### 1.6 每日签到

**LiteSignIn** 提供每日签到与连续奖励（简体中文界面）：

- 进服可弹提醒；GUI 菜单默认开启
- 打开签到界面：

```text
/signin gui
```

签到排名与 PlaceholderAPI 占位符可供菜单/Hologram 展示；奖励组在 `plugins/LiteSignIn/Rewards/` 配置。

---

## 2. 政治学

研究**治权、划界、准入与规则执行**。

### 2.1 领地与划界

主世界与下界以 **Residence** 为主系统：旗标与拒绝文案为**纯中文**，公共领地归属 **MCWWS** 系统账号（非管理员私人号）。

- 无权限时，屏幕顶端 **Boss 栏**提示约 **2 秒**（`MCWWS_ResidenceQuiet`）；同文案未消失前不叠第二条
- 无权限时**真正无法**开箱、开门、扳拉杆等；放置/破坏仍按旗标判定
- 潜行 + 手持物品右键视为放置尝试

**GriefPrevention** 在 **`world`** 与 **`dimensionalhome`** 可作为**第二套**圈地工具（金锹/木棍），下界与末地默认关闭；与 Residence 重叠时，**更严**的一方生效——建议只维护一套主领地以免混淆。断线后可立即重连，无登录冷却（`LoginCooldownSeconds: 0`）。

**WorldGuard** 提供区域 flag，多用于 spawn、活动场与特殊保护，与 Residence 互补。

### 2.2 大规模改造与治权

指令批量与可视化编辑均**逐格**检验领地：

- 纯拆除：仅查「破坏」
- 放置、替换、粘贴、搬运：须同时有「放置」与「破坏」
- 搬运须同时检验源格与目标格；跨多块领地按格分别判断
- 「跳过扣费」**不能**绕过领地；管理员覆盖走 ResAdmin / OP

旗标详情：

```text
/res
```

### 2.3 准入与特权（玩家关心的）

| 能力 | 默认 | 说明 |
| ---- | ---- | ---- |
| 生存中可视化编辑 | 开 | 仍按格扣费 |
| 实体操纵小方块 | **关** | OP 亦无；须单独授予 |
| 跳过可视化编辑扣费 | **关** | 不能绕过领地 |
| 跳过指令批量扣费 | **关** | 不能绕过领地 |
| 指令批量常用能力 | 开 | 不含笔刷、填洞工具等 |
| 连根砍树 | 开 | 无需额外权限 |
| 返回上一记录点 | 开 | `essentials.back`；指南菜单扣费 |
| 个人标记点（设/传） | 开 | `essentials.warp` / `setwarp` / `delwarp` / `warp.overwrite.*` |
| 公共标记点（设） | 开 | `spawn.set`；传送 `/spawn` 本身无权限节点 |
| 外卖大/小编号 | **关** | 仅 OP 或 `mcwws.delivery.admin`；取件码仍人人可用 |

---

## 3. 社会学

研究**人如何结成共同体、如何被引导、如何被承认**。

### 3.1 初入与世界观

1. 在 **world** 与 **印刷部门** 对话，完成「挖 **1** 个石头」  
2. 回导引处领取铁镐与面包  
3. 与 **矿区探险家** 对话，挖 **5** 个煤矿石  
4. 领取铁剑与火把  

任务在聊天栏提示；与 NPC 对话推进。

### 3.2 荣誉、进度与赛季

| 层次 | 你如何接触 |
| ---- | ---------- |
| 世界进度（L 键） | **只显示一页「流浪世界」**：按模块分行、横向延伸（新手入门 / 矿区探索 / 建造与改造）；成就不进 L 键竖树 |
| 成就（聊天 GUI） | `/aach list` 查看完整成就目录；获得时仍有提示，不占 L 键标签页 |
| 本服特色成就 | 「本服特色」分类：倒反天罡（长矛击杀服主 ×10）、谁让你倒反天罡？（完成后被服主击杀）、亿万富翁（收支累计 1 亿）、日活王（连续 64 天日赚 ≥9999）、暗藏悬坤（亮度 0 + 栓绳吊鸡且脚下无方块）；游玩时长成就：网瘾少年 / 中年 / 老年（100 / 1000 / 10000 小时） |
| 赛季任务 | `/battlepass`；中文任务、商店与消息；高级档可用战斗币兑换 |

界面保留常用英文：**NPC、VIP、XP、mcMMO** 等。

> L 键里「一个根进度 = 一个标签页」。原版五个根与成就插件根若同时显示会挤满侧栏；本服关闭原版进度页与成就页，只保留「流浪世界」。成就请用 `/aach list`。

### 3.3 公共指南与协作

右键服务器指南针进入**公共指南**：贸易、传送、功能购买、服务管理等入口集中于此。  
首页有**服务器告示**（书本）：首页写明发布时间（精确到秒）；**每次进服**都会自动打开。也可随时：

```text
/news
```

管理员可在指南「服务器管理」页左键热重载常用服务，无需在聊天重复敲命令。

### 3.4 跨平台与社交

| 功能 | 说明 |
| ---- | ---- |
| Geyser + floodgate | 基岩版玩家 UDP **25565** 进同一 Java 服；Bedrock 名前缀 `.`，支持账号链接 |
| GSit | 坐在方块/玩家/楼梯上社交，见建筑学 · 装饰 |
| ChestSort | 中键或 `/sort`、`/isort` 整理箱子与背包 |
| ZMusic | 点歌/背景音乐（配置于 `plugins/ZMusic/`） |
| FakePlayerPlugin | 氛围假人，不影响经济排行 |
| Back / BackToBody | 死亡回溯与尸体找回 |

**BanItem** 按配置禁用特定物品合成/使用；遇「无法使用该物品」可先查是否被列入黑名单。

# 附录

## 附录 A：玩家常用命令

打开指南针公共指南（无子命令，仅斜杠）：

```text
/
```

```text
/bags open
```

```text
/shop searchgui
```

```text
/axiomrestore
```

```text
/axiomcheck
```

```text
/res
```

```text
/signin gui
```

```text
/news
```

```text
/sort
```

```text
/isort
```

```text
/aach list
```

```text
/battlepass
```

```text
/gsit
```

## 附录 B：管理员命令

经济：

```text
/eco-reload
```

```text
/eco-check
```

```text
/eco-market-reload
```

```text
/eco-market-info
```

```text
/eco-market-reset
```

```text
/eco-market-reset-all
```

服务重载：

```text
/mcwws-we-reload
```

```text
/mcwws-web-reload
```

```text
/mcwws-web-status
```

```text
/mcwws-resquiet-reload
```

```text
/mcwws-bags-reclaim
```

```text
/shop reload
```

```text
/bq reload
```

```text
/aach reload
```

```text
/aach generate
```

```text
/battlepass reload
```

服务器告示（BookNews）：改完 `plugins/BookNews/config.yml` 后重载即可（`always: true`，玩家每次进服都会弹书）。首页第一行须为 `发布：yyyy年MM月dd日 HH:mm:ss`。

```text
/booknews reload
```

授予实体小方块权限：

```text
/lp user <玩家名> permission set mcwws.axiom.survival.entity true
```

## 附录 C：公开 Wiki 建议目录

1. **欢迎与速览**
2. **自然科学**
   - 地理学（空间、测绘、位移、地貌数据包）
   - 物理学（光、采矿力学、飞行）
   - 生物学（采伐、养殖与作物）
   - 化学（辐射、工艺流程、能源化工）
   - 魔法学（附魔、炼金注入、护符、奇术）
   - 合成学（配方、手持规则、大型合成链）
   - 计算机学（电力物流、脚本自动化、终端）
   - 建筑学（生存改造、曲线、轨道交通、装饰）
   - 承载学（随身物品、外卖柜、储物）
3. **社会科学**
   - 经济学（市场、记账、贸易、银行、签到）
   - 政治学（领地、准入、跨平台）
   - 社会学（引导、荣誉、公共指南、社交 QoL）
4. **附录**
   - A 玩家命令 · B 管理员命令 · C 目录 · D 源码对照
   - **E 全插件百科** · **F 全数据包百科**

Halo 嵌入商店页：全宽、藏 TOC，只留商城本体。

## 附录 D：维护者源码对照

改扣费数字、权限默认值或交互步骤时，先改实现再改本文。

| 主题 | 主要位置 |
| ---- | -------- |
| 动态价格 | `plugins/Skript/scripts/mcwws/economy/` |
| 导出物价 | `plugins/Skript/scripts/web/mcwws/economy/web_prices.yml` |
| 指令批量改造扣费 | `tools/mcwws-worldedit-survival/` |
| 可视化编辑扣费 | `tools/mcwws-axiom-survival/`、`tools/mcwws-axiom-survival-client/` |
| 店内贸易补丁 / 仓库 | `tools/mcwws-ultimateshop-fix/`、`tools/mcwws-ultimateshop-stash/` |
| 零钱明细 | `tools/mcwws-economy-ledger/` |
| 网页服务 | `tools/mcwws-web-host/`、`plugins/Skript/scripts/web/` |
| 外卖分配器 / 外卖柜 | `plugins/RykenSlimefunCustomizer/addons/MCWWS-rsc/`、`plugins/Skript/scripts/mcwws/shop/delivery_locker.sk`、`web_pending_delivery.sk` |
| 领地提示 | `tools/mcwws-residence-quiet/` |
| 进服冷却 | `plugins/GriefPreventionData/config.yml`（`Spam.LoginCooldownSeconds`，本服为 0） |
| 指南与传送 | `plugins/DeluxeMenus/gui_menus/guide/`；标记/返回默认权限 `plugins/Skript/scripts/mcwws/utility/guide_marker_perms.sk` |
| 服务器告示（BookNews） | `plugins/BookNews/config.yml`；首页入口 `guide/home.yml` |
| 成就 / 赛季 / 新手引导 | `plugins/AdvancedAchievements/`、`tools/mcwws-idea-achievements/`、`plugins/BattlePass-Fork/`、`plugins/BetonQuest/QuestPackages/mcwws_newbie/` |
| 世界进度 | `tools/mcwws-ultimateadvancements/` |
| 测绘前端 | `bluemap/web/js/mcwws-gis.js` |
| 进度根标题 datapack | `world/datapacks/mcwws_advancement_labels/` |
| 连锁采矿 | `plugins/VeinMiner/` |
| UltimateTimber × ExoticGarden 果树保护 | `tools/mcwws-ultimatetimber-fix/` |
| 魔法 / 炼金附属 | `plugins/AlchimiaVitae/`、`plugins/Alchema/`、`plugins/TranscEndence/` |
| 每日签到 | `plugins/LiteSignIn/` |
| 基岩桥接 | `plugins/Geyser-Spigot/`、`plugins/floodgate/` |
| 轨道交通 | `plugins/Train_Carts/`、`plugins/TCCoasters/` |

## 附录 E：全插件百科

> 下列为 `plugins/` 目录内**已加载 jar**（截至文档修订时约 **140** 个）及主要数据目录说明。  
> **MCWWS_*** 为制作组自研；其余为第三方。`plugins/屏蔽/` 内为**停用**备份，不列入下表。  
> 版本号取自 jar 文件名；无版本号的以目录内 `plugin.yml` 或 jar 构建信息为准。

### E.1 制作组自研（MCWWS / RSC）

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| MCWWS_WorldEditSurvival | — | 生存 WorldEdit 扣费、撤销 95%、50 万格扫描上限 |
| MCWWS_AxiomSurvival | 1.1.7 | 生存 Axiom 扣费、实体操作计价、禁止切创造 |
| MCWWS_UltimateShopStash | — | 替代 UltraDepository 的拾取入库与 60s 豁免 |
| MCWWS_UltimateShopFix | — | 商店 GUI 手持物品防误触 |
| MCWWS_EconomyLedger | — | 零钱明细队列（飞行/WE/Axiom 合并记账） |
| MCWWS_WebHost | — | 自动启动 Node 网页服务（默认 :8002） |
| MCWWS_ResidenceQuiet | — | Residence 拒绝 Boss 栏 2s、Paper 26.2 交互修复 |
| MCWWS_IdeaAchievements | — | 本服特色成就逻辑 |
| MCWWS_UltimateAdvancements | — | L 键仅「流浪世界」单页进度 |
| MCWWS_SFurnaceFix | 1.0.0 | Slimefun 熔炉界面修复 |
| MCWWS_UltimateTimberFix | 1.1.0 | ExoticGarden 果树连根砍兼容（补种果树苗、树叶改掉落） |
| MCWWS-rsc（RSC 附属） | — | 外卖分配器、外卖柜、服内定制 Slimefun 物品 |
| Skript `scripts/mcwws/` | — | 动态经济、传送、光源、便携台拦截等核心脚本 |
| Skript `scripts/web/` | — | 网页商城、账本、物价导出 |
| BetterBags.jar | — | 头颅背包**禁用**（配合 Skript 收回物品） |

### E.2 经济与贸易

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| EssentialsX (+Chat/Spawn/GeoIP) | 2.22.0 | 主钱包 ¥、spawn、聊天格式 |
| Vault | — | 经济 API 桥 |
| BankPlus | 6.4 | 银行账户、利息、转账日志 |
| UltimateShop | 4.7.7 | 主商店；`/shop searchgui`；26.2 创造分类 |
| ajLeaderboards | 2.11.0-b338 | 排行榜 |
| LiteSignIn | 1.9.0.1 | 每日签到 `/signin gui` |
| BossShopPro | — | 旧版 GUI 商店框架（配置保留，UltimateShop 为主） |
| DynamicShop / Nascraft | — | 动态商店/股市插件（配置在，玩家面向以 UltimateShop 为准） |
| UltraDepository | — | **已停用**（jar 在 `屏蔽/`；数据由 MCWWS_UltimateShopStash 迁移） |

### E.3 建造与世界编辑

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| FastAsyncWorldEdit | 2.15.3 | WorldEdit 高性能后端 |
| WorldEdit + WorldEditSUI | 1.8.0 | 选区指令与可视化选区 |
| MCWWS_WorldEditSurvival | — | 见 E.1 |
| AxiomPaper | 5.0.4 MC26.2 | 服务端 Axiom 支持 |
| MCWWS_AxiomSurvival | — | 见 E.1 |
| CurveBuilding | 0.6.3 | 曲线/曲面建造辅助 |
| ColoredAnvils | 2.0.0 | 染色铁砧 |
| AnvilPanel | 1.1.0 | 自定义铁砧面板 GUI |
| CustomCrafting | 4.19.1.0 | 自定义配方浏览器（已去掉与 26.2 不兼容的 ProtocolLib 配方包拦截，避免进服卡死/超时） |
| CreativeManager | — | 创造模式保护（另有停用 jar 在 `屏蔽/`） |
| RandomBlockPlacement | 1.2.0 | 方块朝向随机化 |
| WorldEditSlimefun | — | WE 识别 Slimefun 方块 |

### E.4 领地、保护与记录

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| Residence | 6.0.2.4 | 主领地系统；中文 locale |
| MCWWS_ResidenceQuiet | — | 见 E.1 |
| GriefPrevention | — | 副圈地（world + dimensionalhome）；进服无登录冷却 |
| WorldGuard | 7.0.17 | 区域 flag、spawn 保护 |
| CoreProtect | — | 方块日志与回档（管理） |
| InventoryRollbackPlus | 1.8.4 | 死亡背包回档 |
| BanItem | 3.7 | 物品黑名单 |

### E.5 粘液科技核心与库

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| Slimefun | Beta 3419f04 | 核心科技；zh-CN；辐射开；首次进服指南 |
| GuizhanLibPlugin | Build 62 | 中文 SF 生态库与更新 |
| RykenSlimeCustomizer | b94 | 加载 RSC 自定义附属（含 MCWWS-rsc） |
| SlimeGlue | Build 26 | SF 依赖库 |
| MCWWS_SFurnaceFix | 1.0.0 | 熔炉修复 |
| SFCalc | Build 6 | 配方计算器 |
| SaneCrafting | Build 4 | 配方修复 |

### E.6 粘液科技附属（按主题）

**终局 / 大型扩展：** InfinityExpansion Build 15 · Supreme Build 21 · LiteXpansion Build 30 · TranscEndence Build 11 · DynaTech Build 81 · FluffyMachines Build 35 · FoxyMachines Build 27

**生命 / 农业 / 食物：** Cultivation Build 6 · ExoticGarden Build 17 · GeneticChickengineering Build 8 · SlimyBees Build 1 · Gastronomicon Build 20 · UltimateFoods 1.5.0 · HotbarPets Build 3 · FlowerPower Build 5 · MobCapturer Build 15 · ElectricSpawners Build 5

**化学 / 工艺：** Slimefun 本体 · EcoPower · DynaTech · BedrockTechnology · UltimateFoods · DefensiveTurrets

**魔法 / 战斗 / 装备：** AlchimiaVitae Build 9 · Alchema · PotionExpansion Build 2 · TranscEndence Build 11 · SoulJars Build 9 · SlimefunLuckyBlocks Build 2 · SlimefunWarfare Build 10 · FN-FAL-s-Amplifications Build 14 · Bump Build 40 · DracFun v2.0.10 · FlowerPower Build 5 · InfinityExpansion Build 15 · Supreme Build 21

**工具 / 合成 / 杂项：** SlimeTinker Build 49 · ExtraGear/Tools/Heads · SlimyRepair Build 1 · SlimyTreeTaps Build 2 · ExtraTools Build 6 · Cakecraft Build 1 · SlimefunOreChunks Build 1 · SlimefunVoid Build 1 · SlimefunLuckyBlocks Build 2 · BedrockTechnology · EcoPower Build 1 · PrivateStorage Build 1

**储物 / 装饰：** DyedBackpacks Build 3 · ColoredEnderChests Build 4 · SoulJars Build 9

### E.7 任务、成就与进度

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| BetonQuest | 3.2.0 | 新手包 `mcwws_newbie` |
| AdvancedAchievements | 7.4.2 | `/aach list` 成就 GUI |
| MCWWS_IdeaAchievements | — | 本服特色成就 |
| BattlePass-Fork | 8.2.4 | `/battlepass` 赛季通行证 |
| UltimateAdvancementAPI | 2.8.1 | 进度 API |
| MCWWS_UltimateAdvancements | — | L 键「流浪世界」 |
| CustomAdvancements | — | 额外进度定义 |

### E.8 NPC 与脚本

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| Citizens | 2.0.43-b4231 | NPC 基础 |
| CitizensGui | — | NPC 相关 GUI |
| Denizen | 1.3.3 DEV | NPC 脚本 |
| Sentinel | 2.9.4 | NPC 战斗 |
| Depenizen | 2.1.1 | Denizen 桥接 |
| Skript | 2.16.1 | 61+ 脚本配置 |
| skript-reflect / yaml / placeholders | 2.6.3 等 | Skript 扩展 |
| ScriptBlockPlus | v2.3.3 | 方块脚本触发 |
| ScriptEntityPlus | v1.2.3 | 实体脚本触发 |
| CommandTimer | — | 定时命令 |
| ServerVariables | 3.7.2 | 全局脚本变量 |

### E.9 界面、地图与展示

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| DeluxeMenus | 1.14.1 DEV | **指南针公共指南** |
| BookNews | 6.26 | **服务器告示书本**；每次进服弹出；首页含发布时间；`/news` |
| BlueMap | 5.23 | 三维网页地图 + mcwws-gis.js |
| DecentHolograms | 2.10.1 | 全息文字 |
| TitleManager | — | 标题/Tab 动画 |
| InfiniteScoreboard | 1.3.0 | 侧边栏记分板 |
| AdvancedGUI | 2.2.10 | GUI 框架 |
| ClockGUI | — | 时钟 GUI |
| CommandPrompterPaper | 3.2.0 | 聊天命令提示 |
| ColorGradient | 0.0.5 | 渐变文本 |
| ScreenInMC | — | 游戏内嵌浏览器 |
| PlaceholderAPI | 2.12.3 | 占位符（菜单/全息/签到） |

### E.10 生活便利（QoL）

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| UltimateTimber | 4.10.0 | 连根砍树 150 上限（果树掉落/补种见 MCWWS_UltimateTimberFix） |
| VeinMiner | 2.11.2 | 潜行连锁采 64 格，¥16/次 |
| ChestSort | 14.2.0 | `/sort` `/isort` 整理 |
| BetterBags | — | 扩展背包（本服禁用发放） |
| BackpackPlus | 1.5.6 | 额外背包机制 |
| GSit | 3.5.1 | 坐/躺/趴 |
| FlyWithFood | 2.0.7 | 飞行耗能（账本挂钩） |
| MultitoolPlus | 1.5.17 | 多功能工具 |
| CreeperConfettiPro | 2.2.0 | 苦力怕彩纸 |
| One_day | 1.12-1.20 | 主世界天气同步 |
| SaveItems | — | 指定物品死亡保留 |
| ItemCommand / ItemCommands | 1.3.3 | 右键物品执行命令 |
| ItemEdit | 3.7.10 | 物品编辑 GUI |
| ILoreEdit | 2.7.2 | Lore 模板编辑 |
| ItemNBTEdit | 1.2.3 | NBT 编辑 |
| HeadDB | 6.0.0-rc.2 | 头颅库 |
| PlayerParticles | 8.12 | 粒子特效 |
| Back / BackToBody | — | 死亡回溯/尸体 |
| SetSpawn | 5.5 | 出生点 |
| FakePlayerPlugin | 2.0.3 | 假人氛围 |
| ZMusic | 2.10.3 | 音乐点歌 |
| Coordinates / Stopwatch / KeyWords | — | 坐标、秒表、聊天关键词 |
| CMI | — | 管理工具包（**经济关闭**，Essentials 管钱） |

### E.11 交通

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| TrainCarts | 2.0.1-SNAPSHOT-1734 | 自定义列车 |
| TCCoasters | 2.0.1-SNAPSHOT-408 | 过山车扩展 |

### E.12 跨平台与管理

| 插件 | 版本 | 本服作用 |
| ---- | ---- | -------- |
| Geyser-Spigot | — | 基岩进 Java；UDP 25565 |
| floodgate | — | 基岩认证；用户名前缀 `.` |
| LuckPerms | 5.5.53 | 权限 |
| ClearLag / LagAssist / LagFixer | — | 清实体/性能调优 |
| PluginUpdater | 1.0.11 | 更新检查 |
| PluginPortal | 3.8.6 | 插件分发门户 |
| spark | — | 性能分析（管理） |

### E.13 依赖库（无独立玩法，供他插件调用）

BKCommonLib 2.0.2 · CommandAPI 12.0.0 · ProtocolLib 5.4.1 · NBTAPI 2.16.0 · WolfyUtilities 4.19.0 · CraftaroCore · denizen-reflect 2.4.2 · webizen 0.2.6 · rtag 1.5.17 · ParticleNativeAPI · FoliaCompatibleAPI 1.2.0 · CMILib 1.5.9.9 · bStats / PluginMetrics / faststats

### E.14 停用或待更新（`plugins/屏蔽/`、`plugins/待更新/`）

| 位置 | 内容 |
| ---- | ---- |
| 屏蔽 | Back 1.8.6、CreativeManager、MTimer、SimpleTimer、StopwatchPluginNew、UltraDepository 1.3.9 |
| 待更新 | 旧版 Citizens/Denizen/Sentinel/TrainCarts/BlueMap 等备份 jar |

---

## 附录 F：全数据包百科

### F.1 世界生成与结构（`world/datapacks/*.zip`）

| 数据包 | 版本 | 作用 |
| ------ | ---- | ---- |
| Terralith | 26.2 v2.6.4 | 主世界生物群系与地形 |
| Terratonic | 3.0.27 | 与 Terralith 配套构造 |
| Amplified Nether | 1.2.15 | 下界放大地形 |
| Nullscape | 26.2 v1.2.20 | 末地景观 |
| Structory | 1.3.7 | 散布式结构 |
| Structory Towers | 1.0.17 | 塔楼结构 |

上述 zip 仅挂载在 **`world`**；下界/末地维度通过对应包覆盖 nether/end 生成。

### F.2 服务端与制作组数据包（文件夹）

| 数据包 | 类型 | 作用 |
| ------ | ---- | ---- |
| **bukkit** | Paper 内置 | 插件持久化资源（`pack.mcmeta`: Persistent resources provided by plugins） |
| **mcwws_advancement_labels** | MCWWS 自研 | 原版五根进度标题加「原版 ·」前缀，配合关闭原版 L 键页、仅保留「流浪世界」 |

`mcwws_advancement_labels` 修改路径：`data/minecraft/advancement/{story,adventure,husbandry,nether,end}/root.json`。

### F.3 其他维度

`world_nether`、`world_the_end`、`dimensionalhome` 目录下**无**独立 `datapacks/` 文件夹；维度差异由多世界配置与世界生成包共同决定。
