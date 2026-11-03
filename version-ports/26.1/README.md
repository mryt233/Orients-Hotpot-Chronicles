# Orient's Hotpot Chronicles · 之晗的火锅物语

<!-- 本文件由 version-ports/tools/gen_port_readme.py 生成；要改请改脚本里的数据再生成。 -->

**Minecraft 26.1.x · Fabric + NeoForge · 0.2.1-alpha**

[Download / 下载](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.2.1-alpha) · [Full gameplay guide / 完整玩法](https://github.com/mryt233/Orients-Hotpot-Chronicles#readme) · [Report an issue / 反馈问题](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)

> **Alpha / 测试版**
> Compatibility with other mods and potential bugs have not been thoroughly verified. Back up your saves, try it in a new world first, and report any issues.
>
> 与其他模组的兼容性及潜在 bug 尚未充分验证。请备份存档，优先在新存档中试玩，欢迎反馈问题。

> [!NOTE]
> One Fabric JAR covers Minecraft 26.1, 26.1.1 and 26.1.2. The NeoForge JAR is for **26.1.2 only** (NeoForge has stable builds only for 26.1.2).
>
> 一个 Fabric JAR 同时支持 Minecraft 26.1、26.1.1、26.1.2；NeoForge JAR **只支持 26.1.2**（NeoForge 只给 26.1.2 出了正式版）。

## Requirements · 运行要求

| Loader / 加载器 | Minecraft | Requirement / 要求 | Java | Tested with / 实测 |
| --- | --- | --- | --- | --- |
| Fabric | 26.1, 26.1.1, 26.1.2 | Fabric Loader **0.18.4+**, [Fabric API](https://modrinth.com/mod/fabric-api) **0.143.12+** for your 26.1.x<br>Fabric Loader **0.18.4+**，对应 26.1.x 的 [Fabric API](https://modrinth.com/mod/fabric-api) **0.143.12+** | 25+ | 26.1 + API 0.143.12; 26.1.1 + API 0.145.2; 26.1.2 + API 0.145.4; 26.1.2 + Loader 0.20.1 + API 0.155.3 |
| NeoForge | 26.1.2 only / 仅 26.1.2 | NeoForge **26.1.2.71+** (first stable release)<br>NeoForge **26.1.2.71+**（第一个正式版） | 25+ | NeoForge 26.1.2.71; 26.1.2.126 |

Install on **both client and server**. The prerequisite **orient_framework 0.1.2** is bundled inside each JAR; no separate download is needed.

客户端和服务端**都要装**。前置 **orient_framework 0.1.2** 已内嵌在每个 JAR 里，不用另外下载。

## Installation · 安装

1. **Fabric**: install the Fabric loader and Fabric API for your Minecraft version, put `orients_hotpot_chronicles-26.1-fabric-0.2.1-alpha.jar` into `mods`, and launch with Java 25+.
2. **NeoForge**: install the NeoForge loader for your Minecraft version, put `orients_hotpot_chronicles-26.1-neoforge-0.2.1-alpha.jar` into `mods`, and launch with Java 25+.

1. **Fabric**：安装对应 Minecraft 版本的 Fabric 加载器和 Fabric API，把 `orients_hotpot_chronicles-26.1-fabric-0.2.1-alpha.jar` 放进 `mods` 文件夹，用 Java 25+ 启动。
2. **NeoForge**：安装对应 Minecraft 版本的 NeoForge 加载器，把 `orients_hotpot_chronicles-26.1-neoforge-0.2.1-alpha.jar` 放进 `mods` 文件夹，用 Java 25+ 启动。

## About this port · 移植说明

This is the **Minecraft 26.1.x** port of the published **0.2.1-alpha** (main build: Minecraft 1.21.1). It includes: brick stoves and tall stoves; iron and copper nine-grid, yuanyang and four-grid hotpots with six broths; bamboo chopsticks; hotpot tables, stools and benches; plates, bowls and drinks; dipping-sauce stations; a cutting board, kitchen knife and rolling pin with sliced meats, vegetables, tofu, shrimp, meatballs and the ham-sausage chain; the Hot Pot Guide book; and the hot-reloadable number config (`config/orients_hotpot_chronicles.json`, `/orientshotpot reload`).

本版是已发布的 **0.2.1-alpha**（主版本为 Minecraft 1.21.1）的 **Minecraft 26.1.x 移植版**，包含：灶台与高灶台；铁 / 铜的九宫格锅、鸳鸯锅和田字锅，六种汤底；竹筷；火锅桌、桌凳；盘子、碗与饮料；蘸料台；砧板、菜刀、擀面杖，以及各种肉片、蔬菜、豆腐、虾、丸子和火腿肠加工链；火锅指南；可热重载的数值配置（`config/orients_hotpot_chronicles.json`，`/orientshotpot reload`）。

Differences from the 1.21.1 build / 和 1.21.1 主版本的差异：

- Gameplay, items, recipes and numbers are the same as the 1.21.1 build. Screens use this Minecraft version's own vanilla look (backgrounds, recipe book, creative-tab paging). / 玩法、物品、配方和数值与 1.21.1 主版本相同；界面背景、配方书、创造栏翻页等按本 MC 版本的原版样子显示。

## Test status · 测试范围

Every JAR: 28 automated server checks on the lowest and the newest supported loader, then save, restart and check the world again (all passed). Client: 18 in-game scenes (pots, stoves, chopsticks, plates, sauces, stools, screens, guide book, creative tabs, tooltips) captured on both loaders and compared with the 1.21.1 build. Eating with chopsticks and drinking were checked against vanilla food and potions: the sounds and crumbs during use match.

每个 JAR：在最低和最新的加载器上各跑 28 项服务端自动检查，再存档、重启、重查存档（全部通过）。客户端：两个加载器各截 18 个游戏场景（锅、灶台、筷子、盘子、蘸料、凳子、界面、指南书、创造栏、物品提示），逐张和 1.21.1 主版本对比。用筷子吃、喝饮料时过程中的声音和碎渣，已对照原版食物和药水检查，节奏一致。

Multiplayer with several real players and combinations with other mods have not been tested yet.

多名真人玩家联机、与其他模组搭配还没有测试。

## Other builds · 其他版本

All 0.2.1-alpha files are on [one release page](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.2.1-alpha). Choose the JAR matching your Minecraft version and loader.

0.2.1-alpha 的所有文件都在[同一个发布页](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.2.1-alpha)，按 Minecraft 版本和加载器选择对应的 JAR。

| Minecraft | Loader / 加载器 | File / 文件 | Java |
| --- | --- | --- | --- |
| 1.21.1 | Fabric (main / 主版本) | `orients_hotpot_chronicles-1.21.1-fabric-0.2.1-alpha.jar` | 21+ |
| 1.21.1 | NeoForge | `orients_hotpot_chronicles-1.21.1-neoforge-0.2.1-alpha.jar` | 21+ |
| 26.2 | Fabric | `orients_hotpot_chronicles-26.2-fabric-0.2.1-alpha.jar` | 25+ |
| 26.2 | NeoForge | `orients_hotpot_chronicles-26.2-neoforge-0.2.1-alpha.jar` | 25+ |
| 26.1 – 26.1.2 | Fabric | `orients_hotpot_chronicles-26.1-fabric-0.2.1-alpha.jar` | 25+ |
| 26.1.2 | NeoForge | `orients_hotpot_chronicles-26.1-neoforge-0.2.1-alpha.jar` | 25+ |
| 1.20.1 | Fabric | `orients_hotpot_chronicles-1.20.1-fabric-0.2.1-alpha.jar` | 17+ |
| 1.20.1 | Forge | `orients_hotpot_chronicles-1.20.1-forge-0.2.1-alpha.jar` | 17+ |

Fabric builds need Fabric API for the same Minecraft version; Forge and NeoForge builds do not.

Fabric 版需要同 Minecraft 版本的 Fabric API；Forge 和 NeoForge 版不需要。

## Release files · 发行文件

| File / 文件 | SHA-256 |
| --- | --- |
| `orients_hotpot_chronicles-26.1-fabric-0.2.1-alpha.jar` | `388bf635df758aff6f11b0cc15f7eab5fff05245abd54a22c9832704d2dc3255` |
| `orients_hotpot_chronicles-26.1-neoforge-0.2.1-alpha.jar` | `07c4a66982402c0552158abb5dc77fdd941a8d9ec66c90078db9f511ac72eccf` |

## Build · 构建

This directory is an independent Gradle project: Fabric Loom + ModDevGradle; JDK 25 toolchain (run Gradle with JDK 21). Run `.\gradlew.bat build` (Windows) or `./gradlew build` (Linux/macOS) here; the JARs appear in each loader's `build/libs`.

本目录是独立 Gradle 工程：Fabric Loom + ModDevGradle；Java 工具链 JDK 25（用 JDK 21 运行 Gradle）。在本目录运行 `.\gradlew.bat build`（Windows）或 `./gradlew build`（Linux/macOS），JAR 在各加载器目录的 `build/libs` 里。

## License · 许可证

Code / 源代码：[MIT](LICENSE)。Original non-code assets / 原创非代码资源：[CC BY-NC-SA 4.0](ASSETS_LICENSE)。Minecraft 原版及第三方素材遵循原权利人的条款。

Author / 作者：蘧白川 / Orient · [mryt233](https://github.com/mryt233)
