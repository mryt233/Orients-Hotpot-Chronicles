# 下载、安装与反馈

## 支持的版本 {#versions}

最新版 **0.2.2-alpha** 同步支持 **Minecraft 1.21.1、1.20.1、26.1.x 和 26.2** 的主流加载器，所有 jar 都**内嵌了前置框架 `orient_framework`**，下载一个直接放进 `mods` 就行：

| Minecraft | 加载器 | Java | 还需要装 |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric**（教程用它演示，**0.2.2-alpha**） | 21 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.116.14+1.21.1） |
| **1.21.1** | **NeoForge**（**0.2.2-alpha**） | 21 或更高 | NeoForge 21.1.186 或更高 |
| **1.20.1** | **Fabric**（**0.2.2-alpha**） | 17 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.92.12+1.20.1） |
| **1.20.1** | **Forge**（**0.2.2-alpha**） | 17 或更高 | Forge 47.1 以上（测试用 47.4.28） |
| **26.1.x** | **Fabric / NeoForge**（**0.2.2-alpha**） | 25 或更高 | 对应 26.1.x 的 Fabric Loader + Fabric API，或 NeoForge |
| **26.2** | **Fabric / NeoForge**（**0.2.2-alpha**） | 25 或更高 | 对应 26.2 的 Fabric Loader + Fabric API，或 NeoForge |

:::tip
**本教程用 1.21.1 Fabric 演示。** 0.2.2-alpha 起上述所有版本的玩法、食材、小料、家具和配置功能完全同步；细微底层差异见下面「[各版本有什么不一样](#ports)」。
:::

- **旧版存档与配置兼容**：0.2.2-alpha 将旧版 `tofu` 拆分为 `cold_tofu`（冷豆腐）和 `hot_tofu`（热豆腐），旧存档里的 `config/orients_hotpot_chronicles.json` 中的 `tofu` 配置键会自动无损迁移，升级前仍建议备份存档。
- 1.20.4 Fabric 仍停在旧版 0.1.1-alpha，不再维护。
- 多人联机时，客户端和服务端都要装对应加载器的同版本 jar。

## 去哪里下载 {#download}

| 来源 | 地址 | 说明 |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | 正常用 |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **如果打不开就是在审核**，先用别的渠道 |
| **MC 百科** | <https://www.mcmod.cn/class/32185.html> | 中文模组百科：看介绍、留言讨论 |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | 每个版本一页，里面放各加载器的 jar |

**下载认准两点：Minecraft 版本、加载器。** 文件名里写着，比如 `orients_hotpot_chronicles-1.21.1-fabric-0.2.2-alpha.jar`、`orients_hotpot_chronicles-1.21.1-neoforge-0.2.2-alpha.jar`，选错版本进不去游戏。

## 怎么装 {#install}

### Fabric（1.21.1 / 1.20.1 / 26.1.x / 26.2）

1. 用 [Fabric Loader](https://fabricmc.net/use/) 装出对应版本的游戏。
2. 下载**同版本**的 [Fabric API](https://modrinth.com/mod/fabric-api)。
3. 把 Fabric API 和本模组 jar 放进 `.minecraft/mods`，启动游戏。（如想在模组列表里点「配置」调数值，可顺手装上 **Mod Menu**。）

### NeoForge（1.21.1 / 26.1.x / 26.2）

1. 装好对应 Minecraft 版本的 [NeoForge](https://neoforged.net/)（1.21.1 需 21.1.186 或更高）。
2. 把模组 NeoForge jar 放进 `mods` 文件夹，启动游戏。**不需要 Fabric API。**

### Forge（1.20.1）

1. 装好 [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) 1.20.1（47.1 以上，测试用 47.4.28）。
2. 把模组 Forge jar 放进 `mods` 文件夹，启动游戏。**不需要 Fabric API。**

装好进创造模式物品栏，有「**之晗的火锅物语**」一页，左边有「全部 / 厨具 / 餐具 / 桌凳 / 食物」小标签帮你分类。

:::warn
模组还在 **alpha** 阶段，只做过初步测试。0.1.0-alpha.1 旧存档升级后东西可能丢失或变样，**先备份存档，最好在新存档玩**。
:::

## 各版本有什么不一样 {#ports}

0.2.2-alpha 的 **1.21.1、1.20.1、26.1.x、26.2** 全部对齐了同一套玩法与新内容，只有极少数受原版底层限制的细微差别：

| 项目 | 1.21.1 / 26.1.x / 26.2 | 1.20.1（Fabric / Forge） |
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
