# Orient's Hotpot Chronicles · 之晗的火锅物语

<!-- 本文件由 version-ports/tools/gen_port_readme.py 生成（）；要改请改脚本里的数据再生成。 -->

**Minecraft 1.20.1 · Fabric · 0.1.4-alpha**

[Download / 下载](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.4-alpha) · [Full gameplay guide / 完整玩法](https://github.com/mryt233/Orients-Hotpot-Chronicles#readme) · [Report an issue / 反馈问题](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)

> **Alpha / 测试版**
> Only preliminary testing has been performed. Compatibility with other mods and potential bugs have not been thoroughly verified. Back up your saves, try it in a new world first, and report any issues.
>
> 目前仅做过初步测试，与其他模组的兼容性及潜在 bug 尚未充分验证。请备份存档，优先在新存档中试玩，欢迎反馈问题。
>
> Some items, such as the drinks, have no ingredients or crafting recipes yet and are Creative-only.
>
> 部分物品（比如饮料）暂时没有原材料和合成配方，只能在创造模式物品栏获取。

## Requirements · 运行要求

| Item / 项目 | Requirement / 要求 | Tested with / 测试版本 |
| --- | --- | --- |
| Minecraft | **1.20.1 only / 仅 1.20.1** | 1.20.1 |
| Loader / 加载器 | **Fabric Loader 0.19.3+** | 0.19.3 |
| Fabric API | **Required**, **0.92.12+1.20.1** or newer / **必须**，0.92.12+1.20.1 或更新 | 0.92.12+1.20.1 |
| Java | **17+** | Java 17 |
| Installation / 安装位置 | **Both client and server / 客户端和服务端都要装** | — |

## Installation · 安装

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.20.1**.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for **1.20.1**.
3. Put `orients_hotpot_chronicles-1.20.1-fabric-0.1.4-alpha.jar` into the `mods` folder on both client and server.
4. Launch the Minecraft **1.20.1 Fabric** profile with **Java 17+**.

1. 安装 Minecraft **1.20.1** 对应的 [Fabric Loader](https://fabricmc.net/use/)。
2. 安装 **1.20.1** 对应的 [Fabric API](https://modrinth.com/mod/fabric-api)。
3. 把 `orients_hotpot_chronicles-1.20.1-fabric-0.1.4-alpha.jar` 放进客户端和服务端的 `mods` 文件夹。
4. 选择 Minecraft **1.20.1 Fabric** 启动配置，用 **Java 17+** 启动。

## About this port · 移植说明

This is the **Minecraft 1.20.1 Fabric** port of the published **0.1.4-alpha** (originally made for Minecraft 1.21.1 Fabric). It includes: brick stoves; iron and copper nine-grid, S-shaped / O-shaped / split yuanyang and four-grid hotpots (one broth per side); four broths, bamboo chopsticks, hotpot tables, stools, benches, plates, tripe, bowls and drinks; dipping-sauce stations with nine seasonings and bowls of dipping sauce; a cutting board, iron kitchen knife and rolling pin with fatty beef rolls, mutton rolls, beef loin slices, potato slices, soybeans, tofu and meatballs; scallions and cilantro from grass.

本版是已发布的 **0.1.4-alpha**（原版本为 Minecraft 1.21.1 Fabric）的 **Minecraft 1.20.1 Fabric 移植版**，包含：灶台；铁 / 铜的九宫格锅、S 形 / O 形 / 直隔鸳鸯锅和田字锅（每边一种汤底）；四种汤底、竹筷、火锅桌、桌凳、盘子、毛肚、碗与饮料；蘸料台、九种小料和蘸料；砧板、铁菜刀、擀面杖，以及肥牛卷、羊肉卷、吊龙、土豆片、黄豆、豆腐和丸子；草里掉的小葱、香菜。

Differences from the main 1.21.1 Fabric build / 和 1.21.1 Fabric 主版本的差异：

- Pot steam uses white cloud particles (Minecraft 1.20.1 has no white-smoke particle). / 锅里的热气用白色云粒子（Minecraft 1.20.1 没有白烟粒子）。
- Raw and cooked tripe count as meat (wolves can eat them); in the 1.21.1 Fabric build they do not. / 生毛肚、熟毛肚算肉（狼能吃）；1.21.1 Fabric 主版本不算。
- Worlds from 0.1.0-alpha.1 are not converted (this port starts from 0.1.1-alpha). / 不转换 0.1.0-alpha.1 的旧存档（本移植版从 0.1.1-alpha 开始）。

## Test status · 测试范围

Build; 27 command checks (placement, campfire cooking, a dropped pot keeping its contents, tripe / beef loin / grass drops, sauce stations, cutting board); placing all 10 hotpots and saving four broths; campfire pot stands; only food goes into a pot; cutting potatoes, tofu and beef loin; shift-picking up a pot with its contents; reversed benches joining; the new recipes not clashing with vanilla; chopsticks (shift-open, pick, eat, put back, raw food into the pot) and dipping; flammability of every block face; food values; all 151 IDs; client startup to the main menu with the new pot item renderers. Dedicated server: Fabric Loader 0.19.3 + Fabric API 0.92.12.

构建；27 项命令检查（摆放、篝火烹饪、挖锅保留内容、毛肚 / 牛脊肉 / 草掉落、蘸料台、砧板）；10 种锅摆放和四种汤底存读；篝火上的锅架；锅只收食物；砧板切土豆、豆腐、牛脊肉；空手 Shift 收锅带内容；条凳反向相连；新配方不和原版冲突；筷子（Shift 开锅、夹菜、吃、放回、生食下锅）和蘸料；所有方块逐面可燃；食物数值；151 个 ID；客户端启动到主菜单、新锅物品图标渲染。正式服务端用 Fabric Loader 0.19.3 + Fabric API 0.92.12。

Player interactions, screens, visuals, multiplayer and other-mod combinations still need in-game testing.

右键交互、界面、画面效果、多人联机和其他模组搭配仍需进游戏实测。

## Other builds · 其他版本

Choose the JAR matching your Minecraft version and loader. Download the matching file from the [Releases](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases) page.

请按 Minecraft 版本和加载器选择对应 JAR，到 [Releases / 发布页](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases) 下载和自己游戏对得上的那一个。

| Minecraft | Loader / 加载器 | File / 文件 | Java |
| --- | --- | --- | --- |
| 1.21.1 | Fabric（main / 主版本） | [`orients_hotpot_chronicles-1.21.1-fabric-0.1.4-alpha.jar`](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.4-alpha) | 21+ |
| 1.21.1 | NeoForge | [`orients_hotpot_chronicles-1.21.1-neoforge-0.1.4-alpha.jar`](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.4-alpha) | 21+ |
| 1.20.4 | Fabric | [`orients_hotpot_chronicles-1.20.4-fabric-0.1.1-alpha.jar`](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.20.4-fabric) | 17+ |
| 1.20.1 | Fabric | [`orients_hotpot_chronicles-1.20.1-fabric-0.1.4-alpha.jar`](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.4-alpha) | 17+ |
| 1.20.1 | Forge | [`orients_hotpot_chronicles-1.20.1-forge-0.1.4-alpha.jar`](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.4-alpha) | 17+ |

Fabric builds need [Fabric API](https://modrinth.com/mod/fabric-api) for the same Minecraft version; Forge and NeoForge builds do not.

Fabric 版需要同 Minecraft 版本的 Fabric API；Forge 和 NeoForge 版不需要。

## Release file · 发行文件

`orients_hotpot_chronicles-1.20.1-fabric-0.1.4-alpha.jar`

SHA-256: `df58e140fe2ce134abbd2091c01448a0d38b9384fd60ed71326545dda657cd5e`

## Build · 构建

This directory is an independent Gradle project: Fabric Loom; JDK 17 toolchain (run Gradle with JDK 21). Run `./gradlew build` (Linux/macOS) or `.\gradlew.bat build` (Windows) here. The first build downloads dependencies.

本目录是独立 Gradle 工程：Fabric Loom；Java 工具链 JDK 17（用 JDK 21 运行 Gradle）。在本目录运行 `.\gradlew.bat build`（Windows）或 `./gradlew build`（Linux/macOS），首次构建需要下载依赖。

## License · 许可证

Code / 源代码：[MIT](LICENSE)。Original non-code assets / 原创非代码资源：[CC BY-NC-SA 4.0](ASSETS_LICENSE)。Minecraft 原版及第三方素材遵循原权利人的条款。

Author / 作者：蘧白川 / Orient · [mryt233](https://github.com/mryt233)
