# Orient's Hotpot Chronicles · 之晗的火锅物语

<!-- 本文件由 version-ports/tools/gen_port_readme.py 生成；要改请改脚本里的数据再生成。 -->

**Minecraft 26.2 · Fabric + NeoForge · 0.3.1-alpha**

[Download / 下载](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.3.1-alpha) · [Full gameplay guide / 完整玩法](https://github.com/mryt233/Orients-Hotpot-Chronicles#readme) · [Report an issue / 反馈问题](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)

> **Alpha / 测试版**
> Compatibility with other mods and potential bugs have not been thoroughly verified. Back up your saves, try it in a new world first, and report any issues.
>
> 与其他模组的兼容性及潜在 bug 尚未充分验证。请备份存档，优先在新存档中试玩，欢迎反馈问题。

> [!NOTE]
> The first Fabric API releases for 26.2 omitted required model and renderer modules. A working dedicated server on an earlier Loader does not establish client compatibility.
>
> 早期 26.2 Fabric API 缺少必需的模型与渲染模块；较旧 Loader 能启动独立服，不代表能启动客户端。

## Requirements · 运行要求

| Loader / 加载器 | Minecraft | Requirement / 要求 | Java | 0.3.1-alpha test configurations / 实测组合 |
| --- | --- | --- | --- | --- |
| Fabric | 26.2 | Fabric Loader **0.19.5+**, [Fabric API](https://modrinth.com/mod/fabric-api) **0.152.0+**<br>Fabric Loader **0.19.5+**，[Fabric API](https://modrinth.com/mod/fabric-api) **0.152.0+** | 25+ | `0.19.5` + `0.152.0+26.2`; `0.20.1` + `0.161.0+26.2` |
| NeoForge | 26.2 | NeoForge **26.2.0.57+** (no Fabric API)<br>NeoForge **26.2.0.57+**（不需要 Fabric API） | 25+ | `26.2.0.57` (server + official patched ABI / 独立服＋官方补丁 ABI); `26.2.0.93` (both / 双端) |

Install on **both client and server**. The prerequisite **orient_framework 0.2.2** is bundled inside each JAR; no separate download is needed.

客户端和服务端**都要装**。前置 **orient_framework 0.2.2** 已内嵌在每个 JAR 里，不用另外下载。

## Installation · 安装

1. **Fabric**: install the Fabric loader and Fabric API for your Minecraft version, put `orients_hotpot_chronicles-26.2-fabric-0.3.1-alpha.jar` into `mods`, and launch with Java 25+.
2. **NeoForge**: install the NeoForge loader for your Minecraft version, put `orients_hotpot_chronicles-26.2-neoforge-0.3.1-alpha.jar` into `mods`, and launch with Java 25+.

1. **Fabric**：安装对应 Minecraft 版本的 Fabric 加载器和 Fabric API，把 `orients_hotpot_chronicles-26.2-fabric-0.3.1-alpha.jar` 放进 `mods` 文件夹，用 Java 25+ 启动。
2. **NeoForge**：安装对应 Minecraft 版本的 NeoForge 加载器，把 `orients_hotpot_chronicles-26.2-neoforge-0.3.1-alpha.jar` 放进 `mods` 文件夹，用 Java 25+ 启动。

## About this port · 移植说明

This is the **Minecraft 26.2** port of **0.3.1-alpha** (main build: Minecraft 1.21.1). It includes: Simple and Full gameplay modes (/orientshotpot mode); 4 chicken-feet items; orange trees, 9 crops and 9 wild plants; 1.5-block stone mill with automatic soymilk drainage into the adjacent open wooden vat, and a 3D wooden press lid for making 2 cold tofu; 40 old hotpot table presets consolidated into 2 universal IDs with independently swappable tops and bases and old-save migration; all hotpot tables and 3 prep racks (multi-tier, desktop, hanging) fireproof; a fast-eating challenge and a four-lane rhythm game with 3 data-pack tracks; 71 LabPBR normal/specular texture sets; ambient-occlusion preservation, stove top sync and part-scoped break particles.

本版是 **0.3.1-alpha**（主版本为 Minecraft 1.21.1）的 **Minecraft 26.2 移植版**，包含：精简模式与完整模式切换（/orientshotpot mode）；4 款鸡爪物品；独立橙子树、9 种耕地作物与 9 种野植；1.5 格高小石磨（紧邻空大木桶自动流浆）与带 3D 木压板的开口大木桶（一次压出 2 块冷豆腐，已取消干货泡发）；40 种旧桌预设整合为 2 个通用 ID（桌面与底座可独立换材质，旧桌自动迁移）；全桌与 3 种备菜架（多层、桌面、壁挂）不可燃；快手抢吃与四轨音游，内置 3 首数据包曲目；71 套 LabPBR 法线与高光贴图；材质替换保留环境光遮蔽、高灶顶层同步与局部敲击粒子。

Differences from the 1.21.1 build / 和 1.21.1 主版本的差异：

- Gameplay, items, recipes and numbers are the same as the 1.21.1 build. Screens use this Minecraft version's own vanilla look (backgrounds, recipe book, creative-tab paging). / 玩法、物品、配方和数值与 1.21.1 主版本相同；界面背景、配方书、创造栏翻页等按本 MC 版本的原版样子显示。

## Test status · 测试范围

302 unit tests passed. Four dedicated-server configurations passed restart, registry and save-persistence checks. Five muted single-player smoke runs covered both loaders at the newer versions (Fast and Fancy) plus the minimum Fabric combination (Fast). The minimum Forge-family version was checked against official patched libraries and run on a dedicated server; its client was not launched. REI 26.2.820: 23/23 binary references resolved on each loader, not an in-game integration test. Shaders, live REI/JEI/Jade/Axiom integration, and extended sessions with several real players were not tested.

302 项单测通过。四套独立服均通过两次启动、注册与存档持久化核验；五轮静音单人冒烟覆盖两个加载器的较新版（流畅和高品质），以及 Fabric 最低组合（流畅）。Forge 系最低档核对官方补丁库并通过独立服，未启动最低档客户端。REI 26.2.820 两端各核对 23/23 个二进制调用成员，不是实机联动。未测光影、装入 REI/JEI/Jade/Axiom 的游戏联动或多真人长时间联机。

Client and server must both use 0.3.1-alpha; versions cannot be mixed. Back up saves before upgrading from 0.3.0-alpha.

客户端和服务端必须同用 0.3.1-alpha，不能混连；从 0.3.0-alpha 升级前请备份存档。

## Other builds · 其他版本

All 0.3.1-alpha files are on [one release page](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.3.1-alpha). Choose the JAR matching your Minecraft version and loader.

0.3.1-alpha 的所有文件都在[同一个发布页](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.3.1-alpha)，按 Minecraft 版本和加载器选择对应的 JAR。

| Minecraft | Loader / 加载器 | File / 文件 | Java |
| --- | --- | --- | --- |
| 1.21.1 | Fabric (main / 主版本) | `orients_hotpot_chronicles-1.21.1-fabric-0.3.1-alpha.jar` | 21+ |
| 1.21.1 | NeoForge | `orients_hotpot_chronicles-1.21.1-neoforge-0.3.1-alpha.jar` | 21+ |
| 26.3 | Fabric | `orients_hotpot_chronicles-26.3-fabric-0.3.1-alpha.jar` | 25+ |
| 26.3 | NeoForge | `orients_hotpot_chronicles-26.3-neoforge-0.3.1-alpha.jar` | 25+ |
| 26.2 | Fabric | `orients_hotpot_chronicles-26.2-fabric-0.3.1-alpha.jar` | 25+ |
| 26.2 | NeoForge | `orients_hotpot_chronicles-26.2-neoforge-0.3.1-alpha.jar` | 25+ |
| 26.1 – 26.1.2 | Fabric | `orients_hotpot_chronicles-26.1-fabric-0.3.1-alpha.jar` | 25+ |
| 26.1.2 | NeoForge | `orients_hotpot_chronicles-26.1-neoforge-0.3.1-alpha.jar` | 25+ |
| 1.20.1 | Fabric | `orients_hotpot_chronicles-1.20.1-fabric-0.3.1-alpha.jar` | 17+ |
| 1.20.1 | Forge | `orients_hotpot_chronicles-1.20.1-forge-0.3.1-alpha.jar` | 17+ |

Fabric builds need Fabric API for the same Minecraft version; Forge and NeoForge builds do not.

Fabric 版需要同 Minecraft 版本的 Fabric API；Forge 和 NeoForge 版不需要。

## Release files · 发行文件

| File / 文件 | SHA-256 |
| --- | --- |
| `orients_hotpot_chronicles-26.2-fabric-0.3.1-alpha.jar` | `0117fb9fe8330e1e239a187aead76c0218a759cea7474e9afe85b716f4a6d764` |
| `orients_hotpot_chronicles-26.2-neoforge-0.3.1-alpha.jar` | `8c29176ea4a0ba35ca323ab7ad2f0a1266c52025f4af0e2d95cf4d60c630c973` |

## Build · 构建

This directory is an independent Gradle project: Fabric Loom + ModDevGradle; JDK 25 toolchain (run Gradle with JDK 21). Run `.\gradlew.bat build` (Windows) or `./gradlew build` (Linux/macOS) here; the JARs appear in each loader's `build/libs`.

本目录是独立 Gradle 工程：Fabric Loom + ModDevGradle；Java 工具链 JDK 25（用 JDK 21 运行 Gradle）。在本目录运行 `.\gradlew.bat build`（Windows）或 `./gradlew build`（Linux/macOS），JAR 在各加载器目录的 `build/libs` 里。

## License · 许可证

Code / 源代码：[MIT](LICENSE)。Original non-code assets / 原创非代码资源：[CC BY-NC-SA 4.0](ASSETS_LICENSE)。Minecraft 原版及第三方素材遵循原权利人的条款。

Author / 作者：蘧白川 / Orient · [mryt233](https://github.com/mryt233)
