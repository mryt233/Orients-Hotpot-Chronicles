# 下载、安装与反馈

## 支持的版本 {#versions}

模组一共支持 **2 个 Minecraft 版本、4 种加载器组合**。最新版 **0.1.4-alpha**：

| Minecraft | 加载器 | Java | 还需要装 |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric**（教程用它演示，**0.1.4-alpha**） | 21 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.116.14+1.21.1） |
| 1.21.1 | NeoForge（0.1.3-alpha-hotfix1） | 21 或更高 | NeoForge 21.1.264 或更高 |
| 1.20.1 | Fabric（0.1.3-alpha-hotfix1） | 17 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.92.12+1.20.1） |
| 1.20.1 | Forge（0.1.3-alpha-hotfix1） | 17 或更高 | Forge 47.1 以上（测试用 47.4.28） |

:::tip
**本教程用 1.21.1 Fabric 演示。** 其他三个版本玩法、配方、数值都一样，照着玩就行；少数细小差别见下面「[移植版有什么不一样](#ports)」。
:::

- **0.1.4-alpha 目前只有 1.21.1 Fabric**；1.21.1 NeoForge、1.20.1 Fabric / Forge 还停在 0.1.3-alpha-hotfix1，晚一步跟上。
- 1.20.1 的 Forge 版同一个 jar 也在 NeoForge 1.20.1（47.1.106）服务端做过初步检查，客户端和联机还没验证。
- **1.20.4 Fabric** 有个旧的 0.1.1-alpha 版，已不再更新，教程不包含。
- 客户端和服务端都要装同一个 jar。

## 去哪里下载 {#download}

| 来源 | 地址 | 说明 |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | 正常用 |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **如果打不开就是在审核**，先用别的渠道 |
| **MC 百科** | <https://www.mcmod.cn/class/32185.html> | 中文模组百科：看介绍、留言讨论 |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | 每个版本一页，里面放各加载器的 jar |

**下载认准两点：Minecraft 版本、加载器。** 文件名里写着，比如 `orients_hotpot_chronicles-1.21.1-fabric-0.1.4-alpha.jar`，选错版本进不去游戏。

## 怎么装 {#install}

### Fabric（1.21.1 / 1.20.1）

1. 用 [Fabric Loader](https://fabricmc.net/use/) 装出对应版本的游戏。
2. 下载**同版本**的 [Fabric API](https://modrinth.com/mod/fabric-api)。
3. 把 Fabric API 和本模组 jar 放进 `.minecraft/mods`，启动游戏。

### NeoForge（1.21.1）

1. 装好 [NeoForge](https://neoforged.net/) 21.1.264 或更高。
2. 把模组 NeoForge jar 放进 `mods` 文件夹，启动游戏。**不需要 Fabric API。**

### Forge（1.20.1）

1. 装好 [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) 1.20.1（47.1 以上，测试用 47.4.28）。
2. 把模组 Forge jar 放进 `mods` 文件夹，启动游戏。**不需要 Fabric API。**

装好进创造模式物品栏，有「**之晗的火锅物语**」一页，左边有「全部 / 厨具 / 餐具 / 桌凳 / 食物」小标签帮你分类。

:::warn
模组还在 **alpha** 阶段，只做过初步测试。0.1.0-alpha.1 旧存档升级后东西可能丢失或变样，**先备份存档，最好在新存档玩**。
:::

## 移植版有什么不一样 {#ports}

1.20.1（Fabric / Forge）和 1.21.1 NeoForge 是从 1.21.1 Fabric 版移植的，**玩法、配方、数值、贴图一样**，差别很小：

| 项目 | 1.21.1 Fabric | 1.20.1 |
| --- | --- | --- |
| 锅里冒的热气 | 白烟粒子 | 云粒子（白色往上飘，略大一点） |
| 吃完还碗 | 读食物自带属性 | 照原版规则还碗 |

**新内容通常 Fabric 1.21.1 先有，移植版晚一步跟上**。发布页和教程版本标签会写清。

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
