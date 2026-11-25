<!-- mryt233 改（2026-11-26）：按 0.3.2-alpha 五版本十包同步发布口径更新。 -->
# 下载、安装与反馈

## 支持的版本 {#versions}

最新版 **0.3.2-alpha** 同步支持 **Minecraft 1.21.1、1.20.1、26.1.x、26.2 与 26.3** 共五大版本线（10 个安装包），所有 jar 都**内嵌了前置框架 `orient_framework 0.2.2`**，下载对应版本和加载器的单个 jar 直接放进 `mods` 就行：

| Minecraft | 加载器 | Java | 还需要装 |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric**（教程用它演示，**0.3.2-alpha**） | 21 或更高 | Fabric Loader 0.15.11+、同版 **Fabric API**（实测 0.101.2+1.21.1 / 0.116.14+1.21.1） |
| **1.21.1** | **NeoForge**（**0.3.2-alpha**） | 21 或更高 | NeoForge 21.1.186 或更高 |
| **1.20.1** | **Fabric**（**0.3.2-alpha**） | 17 或更高 | Fabric Loader 0.14.21+、**Fabric API**（0.89.0+1.20.1 起，实测 0.89.0 与 0.92.12） |
| **1.20.1** | **Forge**（**0.3.2-alpha**） | 17 或更高 | Forge 47.0.0 以上（实测 47.0.0 独立服与 47.4.28 双端） |
| **26.1 / 26.1.1 / 26.1.2** | **Fabric**（**0.3.2-alpha**） | 25 或更高 | Fabric Loader 0.19.5+ 与对应 26.1.x 小版本的 **Fabric API**（0.145.0+） |
| **26.1.2** | **NeoForge**（**0.3.2-alpha**） | 25 或更高 | NeoForge 26.1.2.71 或更高 |
| **26.2** | **Fabric / NeoForge**（**0.3.2-alpha**） | 25 或更高 | Fabric Loader 0.19.5+ 与对应 26.2 的 **Fabric API**（0.152.0+），或 NeoForge 26.2.0.57+ |
| **26.3** | **Fabric / NeoForge**（**0.3.2-alpha**） | 25 或更高 | Fabric Loader 0.19.5+ 与对应 26.3 的 **Fabric API**（0.160.3+），或 NeoForge 26.3.0.76+ |

:::tip
**本教程用 1.21.1 Fabric 演示。** 0.3.2-alpha 的全部五个版本线玩法、备菜架叠放与数据包规则、食材、工艺、小游戏、换材质桌架、配置与开放扩展功能保持同步；极少数受原版底层限制的粒子或接口差异见下面「[各版本有什么不一样](#ports)」。
:::

- **旧版存档与配置自动兼容**：
  - 从 `0.2.2-alpha` / `0.3.0-alpha` / `0.3.1-alpha` 升级时，已放置的备菜架与槽位库存（`Slot0..Slot5`）原样保留兼容；早期 **40 种旧预设火锅桌**会在区块加载或进服时无损迁移为统一通用桌并保留原外观，箱内旧桌取出放置同样无损升级，不会出现紫黑方块或丢档；
  - 汤底、蘸料与旧干货泡发废弃配置键（`soaked_*` / `dry_goods_soaking`）会在读取配置时自动静默迁移与清理，重载零警告；升级前仍建议先备份存档。
- **1.20.4 Fabric** 仍停在旧版 0.1.1-alpha，不再维护。
- **多人联机要求**：`0.3.2-alpha` 包含备菜架数据包规则 S2C 同步、模式开关同步、音游曲谱数据包下发与汤底 / 蘸料字符串 ID 同步，**客户端和服务端必须使用同版本 `0.3.2-alpha` jar**，不可与旧版混连。

## 去哪里下载 {#download}

| 来源 | 地址 | 说明 |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | 正常用 |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **如果打不开就是在审核**，先用别的渠道 |
| **MC 百科** | <https://www.mcmod.cn/class/32185.html> | 中文模组百科：看介绍、留言讨论 |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | 每个版本一页，里面放各加载器的 jar |

**下载认准两点：Minecraft 版本、加载器。** 文件名里写着，比如 `orients_hotpot_chronicles-1.21.1-fabric-0.3.2-alpha.jar`、`orients_hotpot_chronicles-1.21.1-neoforge-0.3.2-alpha.jar`、`orients_hotpot_chronicles-26.3-fabric-0.3.2-alpha.jar`，选错版本进不去游戏。

## 怎么装 {#install}

### Fabric（1.21.1 / 1.20.1 / 26.1.x / 26.2 / 26.3）

1. 用 [Fabric Loader](https://fabricmc.net/use/) 装出对应版本的游戏。
2. 下载**同版本**的 [Fabric API](https://modrinth.com/mod/fabric-api)。
3. 把 Fabric API 和本模组 jar 放进 `.minecraft/mods`，启动游戏。（如想在模组列表里点「配置」调数值，可顺手装上 **Mod Menu**。）

### NeoForge（1.21.1 / 26.1.x / 26.2 / 26.3）

1. 装好对应 Minecraft 版本的 [NeoForge](https://neoforged.net/)（1.21.1 需 21.1.186 或更高，26.x 选用对应 MC 版本的 NeoForge）。
2. 把模组 NeoForge jar 放进 `mods` 文件夹，启动游戏。**不需要 Fabric API。**

### Forge（1.20.1）

1. 装好 [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) 1.20.1（47.0.0 及以上，实测 47.0.0 与 47.4.28）。
2. 把模组 Forge jar 放进 `mods` 文件夹，启动游戏。**不需要 Fabric API。**

装好进创造模式物品栏，有「**之晗的火锅物语**」一页，左边有「全部 / 厨具 / 餐具 / 桌凳 / 食物」小标签帮你分类；打开模组设置即可按喜好切换**完整模式**或**精简模式**。

:::warn
模组还在 **alpha** 阶段。从 `0.1.0-alpha.1` 等很早期旧存档升级后东西可能丢失或变样，**先备份存档，最好在新存档玩**。
:::

## 各版本有什么不一样 {#ports}

0.3.2-alpha 的 **1.21.1、1.20.1、26.1.x、26.2、26.3** 全部对齐了同一套玩法与内容，只有极少数受原版底层限制的细微差别：

| 项目 | 1.21.1 / 26.1.x / 26.2 / 26.3 | 1.20.1（Fabric / Forge） |
| --- | --- | --- |
| 锅里冒的热气 | 白烟粒子 | 云粒子（白色往上飘，略大一点） |
| 吃完还碗 | 读食物自带属性 | 照原版与模组规则返还空碗 |

## 反馈、交流 {#feedback}

遇到 bug、有疑问或建议，欢迎告诉作者：

| 渠道 | 联系方式 |
| --- | --- |
| **GitHub Issues** | [提交反馈](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)（报 bug、提建议、提问） |
| **QQ 交流群** | **248314791**（作者没回就是在睡觉） |
| **B 站** | 蘧白川 |
| **邮箱** | orientcheung233@gmail.com |
| 项目主页 | <https://github.com/mryt233/Orients-Hotpot-Chronicles> |

**报 bug 请写清：**
1. 游戏版本、加载器（Fabric / Forge / NeoForge）、模组版本；
2. 做了什么操作、哪一步出问题；
3. 最好带截图，崩溃带上 `latest.log` 或崩溃报告。

模组作者：**蘧白川 / Orient**（之晗和 Orient 是同一人的中英文名）。代码是 MIT 许可证，原创贴图、模型是 CC BY-NC-SA 4.0。
