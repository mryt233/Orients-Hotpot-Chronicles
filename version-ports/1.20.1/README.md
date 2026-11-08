# Orient's Hotpot Chronicles · 之晗的火锅物语

<!-- 本文件由 version-ports/tools/gen_port_readme.py 生成；要改请改脚本里的数据再生成。 -->

**Minecraft 1.20.1 · Fabric + Forge · 0.2.2-alpha**

[Download / 下载](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.2.2-alpha) · [Full gameplay guide / 完整玩法](https://github.com/mryt233/Orients-Hotpot-Chronicles#readme) · [Report an issue / 反馈问题](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)

> **Alpha / 测试版**
> Compatibility with other mods and potential bugs have not been thoroughly verified. Back up your saves, try it in a new world first, and report any issues.
>
> 与其他模组的兼容性及潜在 bug 尚未充分验证。请备份存档，优先在新存档中试玩，欢迎反馈问题。

## Requirements · 运行要求

| Loader / 加载器 | Minecraft | Requirement / 要求 | Java | Tested with (0.2.1-alpha) / 实测（0.2.1-alpha） |
| --- | --- | --- | --- | --- |
| Fabric | 1.20.1 | Fabric Loader **0.14.21+**, [Fabric API](https://modrinth.com/mod/fabric-api) **0.83.0+**<br>Fabric Loader **0.14.21+**，[Fabric API](https://modrinth.com/mod/fabric-api) **0.83.0+** | 17+ | Loader 0.14.21 + API 0.83.0; Loader 0.20.1 + API 0.92.12 |
| Forge | 1.20.1 | Forge **47.0.0+** (no Fabric API)<br>Forge **47.0.0+**（不需要 Fabric API） | 17+ | Forge 47.0.0; Forge 47.4.28 |

Install on **both client and server**. The prerequisite **orient_framework 0.1.3** is bundled inside each JAR; no separate download is needed.

客户端和服务端**都要装**。前置 **orient_framework 0.1.3** 已内嵌在每个 JAR 里，不用另外下载。

## Installation · 安装

1. **Fabric**: install the Fabric loader and Fabric API for your Minecraft version, put `orients_hotpot_chronicles-1.20.1-fabric-0.2.2-alpha.jar` into `mods`, and launch with Java 17+.
2. **Forge**: install the Forge loader for your Minecraft version, put `orients_hotpot_chronicles-1.20.1-forge-0.2.2-alpha.jar` into `mods`, and launch with Java 17+.

1. **Fabric**：安装对应 Minecraft 版本的 Fabric 加载器和 Fabric API，把 `orients_hotpot_chronicles-1.20.1-fabric-0.2.2-alpha.jar` 放进 `mods` 文件夹，用 Java 17+ 启动。
2. **Forge**：安装对应 Minecraft 版本的 Forge 加载器，把 `orients_hotpot_chronicles-1.20.1-forge-0.2.2-alpha.jar` 放进 `mods` 文件夹，用 Java 17+ 启动。

## About this port · 移植说明

This is the **Minecraft 1.20.1** port of the published **0.2.2-alpha** (main build: Minecraft 1.21.1). It includes: brick stoves and tall stoves; iron and copper nine-grid, yuanyang and four-grid hotpots with six broths; bamboo chopsticks; hotpot tables, stools and benches; plates, bowls and drinks; dipping-sauce stations; a cutting board, kitchen knife and rolling pin with sliced meats, vegetables, tofu, shrimp, meatballs, the ham-sausage chain and the 0.2.2-alpha additions (crispy pork, cold / hot tofu, 15 seasonings, four spice levels for the red broth, the Cloth Apron, toasting, chopstick rests, climbable benches, 24 advancements); the Hot Pot Guide book with full-text search; the in-game settings screen; optional JEI / REI / Jade support; and the hot-reloadable number config (`config/orients_hotpot_chronicles.json`, `/orientshotpot reload`).

本版是已发布的 **0.2.2-alpha**（主版本为 Minecraft 1.21.1）的 **Minecraft 1.20.1 移植版**，包含：灶台与高灶台；铁 / 铜的九宫格锅、鸳鸯锅和田字锅，六种汤底；竹筷；火锅桌、桌凳；盘子、碗与饮料；蘸料台；砧板、菜刀、擀面杖，以及各种肉片、蔬菜、豆腐、虾、丸子、火腿肠加工链和 0.2.2-alpha 新内容（酥肉、冷热豆腐、15 种小料、红汤 4 档辣度、布围裙、碰杯、迷你筷架、可攀爬条凳、24 条进度）；可全文搜索的火锅指南；游戏内设置界面；可选的 JEI / REI / Jade 适配；可热重载的数值配置（`config/orients_hotpot_chronicles.json`，`/orientshotpot reload`）。

Differences from the 1.21.1 build / 和 1.21.1 主版本的差异：

- Gameplay, items, recipes and numbers are the same as the 1.21.1 build. Screens use this Minecraft version's own vanilla look (backgrounds, recipe book, creative-tab paging). / 玩法、物品、配方和数值与 1.21.1 主版本相同；界面背景、配方书、创造栏翻页等按本 MC 版本的原版样子显示。
- Pot steam uses the vanilla smoke particle tinted to match 1.21.1's white smoke (1.20.1 has no white-smoke particle). / 锅里的热气用原版烟粒子调成 1.21.1 白烟的颜色（1.20.1 没有白烟粒子）。

## Test status · 测试范围

0.2.2-alpha: every JAR builds and passes the automated resource checks (assets, language keys, recipes and advancements match the 1.21.1 build). Not every new feature has been fully tested in game yet. The "Tested with" column above lists the loader versions tested for 0.2.1-alpha.

0.2.2-alpha：每个 JAR 都能构建，并通过资源自动检查（贴图模型、语言键、配方、进度与 1.21.1 主版本一致）。新内容还没有在游戏里逐项完整测试。上表「实测」一列是 0.2.1-alpha 测过的加载器版本。

Multiplayer with several real players and combinations with other mods have not been tested yet.

多名真人玩家联机、与其他模组搭配还没有测试。

## Other builds · 其他版本

All 0.2.2-alpha files are on [one release page](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.2.2-alpha). Choose the JAR matching your Minecraft version and loader.

0.2.2-alpha 的所有文件都在[同一个发布页](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.2.2-alpha)，按 Minecraft 版本和加载器选择对应的 JAR。

| Minecraft | Loader / 加载器 | File / 文件 | Java |
| --- | --- | --- | --- |
| 1.21.1 | Fabric (main / 主版本) | `orients_hotpot_chronicles-1.21.1-fabric-0.2.2-alpha.jar` | 21+ |
| 1.21.1 | NeoForge | `orients_hotpot_chronicles-1.21.1-neoforge-0.2.2-alpha.jar` | 21+ |
| 26.2 | Fabric | `orients_hotpot_chronicles-26.2-fabric-0.2.2-alpha.jar` | 25+ |
| 26.2 | NeoForge | `orients_hotpot_chronicles-26.2-neoforge-0.2.2-alpha.jar` | 25+ |
| 26.1 – 26.1.2 | Fabric | `orients_hotpot_chronicles-26.1-fabric-0.2.2-alpha.jar` | 25+ |
| 26.1.2 | NeoForge | `orients_hotpot_chronicles-26.1-neoforge-0.2.2-alpha.jar` | 25+ |
| 1.20.1 | Fabric | `orients_hotpot_chronicles-1.20.1-fabric-0.2.2-alpha.jar` | 17+ |
| 1.20.1 | Forge | `orients_hotpot_chronicles-1.20.1-forge-0.2.2-alpha.jar` | 17+ |

Fabric builds need Fabric API for the same Minecraft version; Forge and NeoForge builds do not.

Fabric 版需要同 Minecraft 版本的 Fabric API；Forge 和 NeoForge 版不需要。

## Release files · 发行文件

| File / 文件 | SHA-256 |
| --- | --- |
| `orients_hotpot_chronicles-1.20.1-fabric-0.2.2-alpha.jar` | `f72cc8d4ce236e8229dcff99cdf92bf1c1814e92711af7bb1c03a2b478adf0f5` |
| `orients_hotpot_chronicles-1.20.1-forge-0.2.2-alpha.jar` | `ef8815de130a137b59f127dda019f28930deab6cae1b156d47c3b884a6aaec59` |

## Build · 构建

This directory is an independent Gradle project: Fabric Loom + ModDevGradle LegacyForge; JDK 17 toolchain (run Gradle with JDK 21). Run `.\gradlew.bat build` (Windows) or `./gradlew build` (Linux/macOS) here; the JARs appear in each loader's `build/libs`.

本目录是独立 Gradle 工程：Fabric Loom + ModDevGradle LegacyForge；Java 工具链 JDK 17（用 JDK 21 运行 Gradle）。在本目录运行 `.\gradlew.bat build`（Windows）或 `./gradlew build`（Linux/macOS），JAR 在各加载器目录的 `build/libs` 里。

## License · 许可证

Code / 源代码：[MIT](LICENSE)。Original non-code assets / 原创非代码资源：[CC BY-NC-SA 4.0](ASSETS_LICENSE)。Minecraft 原版及第三方素材遵循原权利人的条款。

Author / 作者：蘧白川 / Orient · [mryt233](https://github.com/mryt233)
