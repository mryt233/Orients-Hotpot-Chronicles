# 下载、安装与反馈

## 支持的版本 {#versions}

模组一共支持 **2 个 Minecraft 版本、4 种加载器组合**，目前都是 **0.1.2-alpha**：

| Minecraft | 加载器 | Java | 还需要装 |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric**（本教程用它演示） | 21 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.116.14+1.21.1） |
| 1.21.1 | NeoForge | 21 或更高 | NeoForge 21.1.264 或更高（**不需要** Fabric API） |
| 1.20.1 | Fabric | 17 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.92.12+1.20.1） |
| 1.20.1 | Forge | 17 或更高 | Forge 47.1 以上（测试用的是 47.4.28，**不需要** Fabric API） |

:::tip
**本教程用 1.21.1 Fabric 演示。** 其他三个版本的玩法、配方、数值都一样，界面和操作照着看就行；少数很小的差别见下面「[移植版有什么不一样](#ports)」。
:::

- 1.20.1 的 Forge 版同一个 jar，也在 NeoForge 1.20.1（47.1.106）的服务端上做过初步检查，客户端和联机还没验证。
- **1.20.4 Fabric** 有一个旧的 0.1.1-alpha 版本，已经不再更新，教程不覆盖它。
- 模组每个版本的客户端和服务端都要装同一个 jar。

## 去哪里下载 {#download}

| 来源 | 地址 | 说明 |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | 正常使用 |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **如果打不开，就是还在审核**，请先用别的渠道 |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | 每个 Minecraft 版本一页，里面同时放 Fabric 和 Forge / NeoForge 的 jar |

GitHub 上的两个发布页：

- [0.1.2-alpha · Minecraft 1.21.1 · Fabric + NeoForge](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.2-alpha-mc1.21.1)
- [0.1.2-alpha · Minecraft 1.20.1 · Fabric + Forge](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.2-alpha-mc1.20.1)

**下载时认准两点：Minecraft 版本、加载器。** 文件名里写着，例如 `orients_hotpot_chronicles-1.21.1-fabric-0.1.2-alpha.jar`，选错版本或加载器，游戏会直接报错进不去。

## 怎么装 {#install}

### Fabric（1.21.1 / 1.20.1）

1. 用 [Fabric Loader](https://fabricmc.net/use/) 装出对应 Minecraft 版本的游戏档案。
2. 下载**同一个 Minecraft 版本**的 [Fabric API](https://modrinth.com/mod/fabric-api)。
3. 把 Fabric API 和本模组的 jar 都放进 `.minecraft/mods` 文件夹，启动游戏。

### NeoForge（1.21.1）

1. 装好 [NeoForge](https://neoforged.net/) 21.1.264 或更高版本。
2. 把本模组的 NeoForge jar 放进 `mods` 文件夹，启动游戏。**不需要 Fabric API。**

### Forge（1.20.1）

1. 装好 [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) 1.20.1（47.1 以上，测试用 47.4.28）。
2. 把本模组的 Forge jar 放进 `mods` 文件夹，启动游戏。**不需要 Fabric API。**

装好后，进游戏的创造模式物品栏，里面有「**之晗的火锅物语**」一页，左边还有「厨具 / 餐具 / 桌子 / 凳子 / 食物」的小标签帮你分类。

:::warn
模组还在 **alpha** 阶段，目前**只做过初步测试**，和其他模组的兼容、多人联机、少见的操作可能有 bug。
0.1.0-alpha.1 的旧存档升级后，本模组的东西可能丢失或变样。**先备份存档，最好在新存档里玩。**
:::

## 移植版有什么不一样 {#ports}

1.20.1（Fabric / Forge）和 1.21.1 NeoForge 都是从 1.21.1 Fabric 版移植的，**玩法、配方、数值、贴图一样**。玩家能感觉到的差别很小：

| 项目 | 1.21.1 Fabric | 1.20.1 |
| --- | --- | --- |
| 锅里冒的热气 | 白烟粒子 | 云粒子（也是白色往上飘，略大一点） |
| 蘑菇煲之类吃完还碗 | 读食物自带属性 | 照原版规则还碗 |

另外要注意：**新内容通常 Fabric 1.21.1 先有，移植版晚一步跟上**。发布页和本教程的版本标签会写清楚。

## 反馈、交流 {#feedback}

遇到 bug、有疑问或建议，欢迎告诉作者：

| 渠道 | 联系方式 |
| --- | --- |
| **GitHub Issues** | [提交反馈](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)（有报 bug、提建议、提问三种表单） |
| **QQ 交流群** | **248314791**（作者没回就是在睡觉） |
| **B 站** | 蘧白川 |
| **邮箱** | orientcheung233@gmail.com |
| 项目主页 | <https://github.com/mryt233/Orients-Hotpot-Chronicles> |

**报 bug 时请写清楚：**
1. Minecraft 版本、加载器（Fabric / Forge / NeoForge）、模组版本；
2. 做了什么操作、哪一步出的问题；
3. 最好带截图，崩溃的话带上 `latest.log` 或崩溃报告。

模组作者：**蘧白川 / Orient**（之晗和 Orient 是同一个人的中文名和英文名）。代码是 MIT 许可证，原创的贴图、模型等资源是 CC BY-NC-SA 4.0。
