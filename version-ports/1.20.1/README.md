# Orient's Hotpot Chronicles · 之晗的火锅物语

<!-- 本文件由 version-ports/tools/gen_port_readme.py 生成；要改请改脚本里的数据再生成。 -->

**Minecraft 1.20.1 · Fabric + Forge · 0.3.0-alpha**

[Download / 下载](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.3.0-alpha) · [Full gameplay guide / 完整玩法](https://github.com/mryt233/Orients-Hotpot-Chronicles#readme) · [Report an issue / 反馈问题](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)

> **Alpha / 测试版**
> Compatibility with other mods and potential bugs have not been thoroughly verified. Back up your saves, try it in a new world first, and report any issues.
>
> 与其他模组的兼容性及潜在 bug 尚未充分验证。请备份存档，优先在新存档中试玩，欢迎反馈问题。

## Requirements · 运行要求

| Loader / 加载器 | Minecraft | Requirement / 要求 | Java | Loader tests (0.2.1-alpha unless noted) / 加载器测试记录（除注明外为 0.2.1-alpha） |
| --- | --- | --- | --- | --- |
| Fabric | 1.20.1 | Fabric Loader **0.14.21+**, [Fabric API](https://modrinth.com/mod/fabric-api) **0.83.0+**<br>Fabric Loader **0.14.21+**，[Fabric API](https://modrinth.com/mod/fabric-api) **0.83.0+** | 17+ | 0.3.0-alpha: Loader 0.14.21 + API 0.83.0; Loader 0.20.1 |
| Forge | 1.20.1 | Forge **47.0.0+** (no Fabric API)<br>Forge **47.0.0+**（不需要 Fabric API） | 17+ | 0.3.0-alpha: Forge 47.0.0; Forge 47.4.28 |

Install on **both client and server**. The prerequisite **orient_framework 0.2.0** is bundled inside each JAR; no separate download is needed.

客户端和服务端**都要装**。前置 **orient_framework 0.2.0** 已内嵌在每个 JAR 里，不用另外下载。

## Installation · 安装

1. **Fabric**: install the Fabric loader and Fabric API for your Minecraft version, put `orients_hotpot_chronicles-1.20.1-fabric-0.3.0-alpha.jar` into `mods`, and launch with Java 17+.
2. **Forge**: install the Forge loader for your Minecraft version, put `orients_hotpot_chronicles-1.20.1-forge-0.3.0-alpha.jar` into `mods`, and launch with Java 17+.

1. **Fabric**：安装对应 Minecraft 版本的 Fabric 加载器和 Fabric API，把 `orients_hotpot_chronicles-1.20.1-fabric-0.3.0-alpha.jar` 放进 `mods` 文件夹，用 Java 17+ 启动。
2. **Forge**：安装对应 Minecraft 版本的 Forge 加载器，把 `orients_hotpot_chronicles-1.20.1-forge-0.3.0-alpha.jar` 放进 `mods` 文件夹，用 Java 17+ 启动。

## About this port · 移植说明

This is the **Minecraft 1.20.1** port of the published **0.3.0-alpha** (main build: Minecraft 1.21.1). It includes: data-pack and submod registration for broths and dipping sauces; a universal soup bowl; data-driven pot boiling and pounding; redesigned common and advanced settings with 37 new options (4 warmth, 2 cold pot, 7 cheers, 10 spice and 14 loot); three aprons retain durability and enchantments when recolored; improved bamboo chopsticks on tables and bowl holders; single-column sauce-station paging; sesame sauce and oyster sauce on tables; iron knife orientation, drink placement sound and author recommendation fixes.

本版是已发布的 **0.3.0-alpha**（主版本为 Minecraft 1.21.1）的 **Minecraft 1.20.1 移植版**，包含：开放汤底和蘸料的数据包与子模组登记；通用汤碗；锅中专煮和捣制数据化；重做常用及高级设置，新增 37 项配置（回血 4、冷锅 2、干杯 7、辣度 10、掉率 14）；三款围裙重染保留耐久和附魔；竹筷摆桌及架碗优化；蘸料台单列翻页；芝麻酱和蚝油摆桌；修复铁菜刀朝向、饮料摆桌音效与作者推荐。

Differences from the 1.21.1 build / 和 1.21.1 主版本的差异：

- Gameplay, items, recipes and numbers are the same as the 1.21.1 build. Screens use this Minecraft version's own vanilla look (backgrounds, recipe book, creative-tab paging). / 玩法、物品、配方和数值与 1.21.1 主版本相同；界面背景、配方书、创造栏翻页等按本 MC 版本的原版样子显示。
- Pot steam uses the vanilla smoke particle tinted to match 1.21.1's white smoke (1.20.1 has no white-smoke particle). / 锅里的热气用原版烟粒子调成 1.21.1 白烟的颜色（1.20.1 没有白烟粒子）。

## Test status · 测试范围

0.3.0-alpha: all unit tests passed; both loaders passed server restart and persistence checks and client smoke tests. For 1.20.1, the minimum Fabric Loader 0.14.21 + API 0.83.0 and Forge 47.0.0 were also tested online. REI and NeoForge JEI / Jade received compilation and interface checks only; extended multiplayer with several real players remains untested.

0.3.0-alpha：全部单元测试通过，双加载器服务端重启持久化及客户端冒烟通过。1.20.1 已联网实测最低 Fabric Loader 0.14.21 + API 0.83.0 和 Forge 47.0.0。REI 与 NeoForge 版 JEI / Jade 仅编译及接口核查，多真人长时间联机仍未测试。

Client and server must both use 0.3.0-alpha; versions cannot be mixed. Back up saves before upgrading from 0.2.2-alpha.

客户端和服务端必须同用 0.3.0-alpha，不能混连；从 0.2.2-alpha 升级前请备份存档。

## Other builds · 其他版本

All 0.3.0-alpha files are on [one release page](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.3.0-alpha). Choose the JAR matching your Minecraft version and loader.

0.3.0-alpha 的所有文件都在[同一个发布页](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.3.0-alpha)，按 Minecraft 版本和加载器选择对应的 JAR。

| Minecraft | Loader / 加载器 | File / 文件 | Java |
| --- | --- | --- | --- |
| 1.21.1 | Fabric (main / 主版本) | `orients_hotpot_chronicles-1.21.1-fabric-0.3.0-alpha.jar` | 21+ |
| 1.21.1 | NeoForge | `orients_hotpot_chronicles-1.21.1-neoforge-0.3.0-alpha.jar` | 21+ |
| 26.2 | Fabric | `orients_hotpot_chronicles-26.2-fabric-0.3.0-alpha.jar` | 25+ |
| 26.2 | NeoForge | `orients_hotpot_chronicles-26.2-neoforge-0.3.0-alpha.jar` | 25+ |
| 26.1 – 26.1.2 | Fabric | `orients_hotpot_chronicles-26.1-fabric-0.3.0-alpha.jar` | 25+ |
| 26.1.2 | NeoForge | `orients_hotpot_chronicles-26.1-neoforge-0.3.0-alpha.jar` | 25+ |
| 1.20.1 | Fabric | `orients_hotpot_chronicles-1.20.1-fabric-0.3.0-alpha.jar` | 17+ |
| 1.20.1 | Forge | `orients_hotpot_chronicles-1.20.1-forge-0.3.0-alpha.jar` | 17+ |

Fabric builds need Fabric API for the same Minecraft version; Forge and NeoForge builds do not.

Fabric 版需要同 Minecraft 版本的 Fabric API；Forge 和 NeoForge 版不需要。

## Release files · 发行文件

| File / 文件 | SHA-256 |
| --- | --- |
| `orients_hotpot_chronicles-1.20.1-fabric-0.3.0-alpha.jar` | `64a9ef5a27553022be6671601d61d37abbcef5f00f76c1f336d5d62a737df237` |
| `orients_hotpot_chronicles-1.20.1-forge-0.3.0-alpha.jar` | `dbedfd88032de602328c9b4ef593953d24472901d2403f0fd437fc256f618bdd` |

## Build · 构建

This directory is an independent Gradle project: Fabric Loom + ModDevGradle LegacyForge; JDK 17 toolchain (run Gradle with JDK 21). Run `.\gradlew.bat build` (Windows) or `./gradlew build` (Linux/macOS) here; the JARs appear in each loader's `build/libs`.

本目录是独立 Gradle 工程：Fabric Loom + ModDevGradle LegacyForge；Java 工具链 JDK 17（用 JDK 21 运行 Gradle）。在本目录运行 `.\gradlew.bat build`（Windows）或 `./gradlew build`（Linux/macOS），JAR 在各加载器目录的 `build/libs` 里。

## License · 许可证

Code / 源代码：[MIT](LICENSE)。Original non-code assets / 原创非代码资源：[CC BY-NC-SA 4.0](ASSETS_LICENSE)。Minecraft 原版及第三方素材遵循原权利人的条款。

Author / 作者：蘧白川 / Orient · [mryt233](https://github.com/mryt233)
