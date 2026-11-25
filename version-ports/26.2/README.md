# Orient's Hotpot Chronicles · 之晗的火锅物语

<!-- 作者：mryt233；本文件由移植说明生成脚本维护。 -->

**Minecraft 26.2 · Fabric + NeoForge · 0.3.2-alpha**

[Releases / 发布页](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases) · [Full gameplay guide / 完整玩法](https://github.com/mryt233/Orients-Hotpot-Chronicles#readme) · [Report an issue / 反馈问题](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)

**0.3.2-alpha：本地归档，待终审。 / Archived locally, pending final review.**

> **Alpha / 测试版**
> Compatibility with other mods and potential bugs have not been thoroughly verified. Back up your saves, try it in a new world first, and report any issues.
>
> 与其他模组的兼容性及潜在 bug 尚未充分验证。请备份存档，优先在新存档中试玩，欢迎反馈问题。

> [!NOTE]
> The first Fabric API releases for 26.2 omitted required model and renderer modules. A working dedicated server on an earlier Loader does not establish client compatibility.
>
> 早期 26.2 Fabric API 缺少必需的模型与渲染模块；较旧 Loader 能启动独立服，不代表能启动客户端。

## Requirements · 运行要求

| Loader / 加载器 | Minecraft | Requirement / 要求 | Java | 0.3.2-alpha dedicated server / 独立服 |
| --- | --- | --- | --- | --- |
| Fabric | 26.2 | Fabric Loader **0.19.5+**, [Fabric API](https://modrinth.com/mod/fabric-api) **0.152.0+**<br>Fabric Loader **0.19.5+**，[Fabric API](https://modrinth.com/mod/fabric-api) **0.152.0+** | 25+ | MC `26.2` · `0.20.1` + `0.161.0+26.2` |
| NeoForge | 26.2 | NeoForge **26.2.0.57+** (no Fabric API)<br>NeoForge **26.2.0.57+**（不需要 Fabric API） | 25+ | MC `26.2` · `26.2.0.93` |

Install on **both client and server**. The prerequisite **orient_framework 0.2.2** is bundled inside each JAR; no separate download is needed.

客户端和服务端**都要装**。前置 **orient_framework 0.2.2** 已内嵌在每个 JAR 里，不用另外下载。

## Installation · 安装

1. **Fabric**: install the Fabric loader and Fabric API for your Minecraft version, put `orients_hotpot_chronicles-26.2-fabric-0.3.2-alpha.jar` into `mods`, and launch with Java 25+.
2. **NeoForge**: install the NeoForge loader for your Minecraft version, put `orients_hotpot_chronicles-26.2-neoforge-0.3.2-alpha.jar` into `mods`, and launch with Java 25+.

1. **Fabric**：安装对应 Minecraft 版本的 Fabric 加载器和 Fabric API，把 `orients_hotpot_chronicles-26.2-fabric-0.3.2-alpha.jar` 放进 `mods` 文件夹，用 Java 25+ 启动。
2. **NeoForge**：安装对应 Minecraft 版本的 NeoForge 加载器，把 `orients_hotpot_chronicles-26.2-neoforge-0.3.2-alpha.jar` 放进 `mods` 文件夹，用 Java 25+ 启动。

## About this port · 移植说明

This is the **Minecraft 26.2** port of **0.3.2-alpha** (main build: Minecraft 1.21.1). It includes: Simple and Full gameplay modes (/orientshotpot mode); 4 chicken-feet items; orange trees, 9 crops and 9 wild plants; 1.5-block stone mill with automatic soymilk drainage into the adjacent open wooden vat, and a 3D wooden press lid for making 2 cold tofu; 40 old hotpot table presets consolidated into 2 universal IDs with independently swappable tops and bases and old-save migration; all hotpot tables and 3 prep racks (multi-tier, desktop, hanging) fireproof; a fast-eating challenge and a four-lane rhythm game with 3 data-pack tracks; 71 LabPBR normal/specular texture sets; ambient-occlusion preservation, stove top sync and part-scoped break particles.

本版是 **0.3.2-alpha**（主版本为 Minecraft 1.21.1）的 **Minecraft 26.2 移植版**，包含：精简模式与完整模式切换（/orientshotpot mode）；4 款鸡爪物品；独立橙子树、9 种耕地作物与 9 种野植；1.5 格高小石磨（紧邻空大木桶自动流浆）与带 3D 木压板的开口大木桶（一次压出 2 块冷豆腐，已取消干货泡发）；40 种旧桌预设整合为 2 个通用 ID（桌面与底座可独立换材质，旧桌自动迁移）；全桌与 3 种备菜架（多层、桌面、壁挂）不可燃；快手抢吃与四轨音游，内置 3 首数据包曲目；71 套 LabPBR 法线与高光贴图；材质替换保留环境光遮蔽、高灶顶层同步与局部敲击粒子。

The three prep racks accept any non-empty item, stack identical items, and use five display layouts. Capacity and layout come from reloadable data-pack rules; drawing limits are configurable. Sneaking with a valid material changes only the rack material. Breaking a rack drops its inventory and the rack separately.

三种备菜架可存任意非空物品、合并同物同数据的堆叠，并按五种布局展示。容量与布局支持数据包热重载，绘制上限可配置；潜行持合格材质只换材质。破坏时库存与架体分别掉落。

Differences from the 1.21.1 build / 和 1.21.1 主版本的差异：

- Gameplay, items, recipes and numbers are the same as the 1.21.1 build. Screens use this Minecraft version's own vanilla look (backgrounds, recipe book, creative-tab paging). / 玩法、物品、配方和数值与 1.21.1 主版本相同；界面背景、配方书、创造栏翻页等按本 MC 版本的原版样子显示。

## Test status · 测试范围

308 unit tests passed. Both final JARs were started on dedicated servers; seven prep-rack rules compiled, both rack settings loaded, and the servers saved and exited normally. The table lists the runs made on these exact JARs, not the minimum configurations. The requirements carry forward the package metadata and the previous release's compatibility checks.

308 项单测通过。双端均用落盘终包启动独立服，读到 7 条备菜架规则和两项配置，正常保存退出。表中只列这两份终包实跑的档位，不把旧版最低组合当作本版实测；下限沿用包内声明及上版兼容验证。

Client and server must both use 0.3.2-alpha; versions cannot be mixed. Back up saves before upgrading.

客户端和服务端必须同用 0.3.2-alpha，不能混连；升级前请备份存档。

## Other builds · 其他版本

All ten JARs are prepared for a single 0.3.2-alpha release. Choose the JAR matching your Minecraft version and loader.

十个 JAR 已按 0.3.2-alpha 的同一发布页备齐，终审前不会上传；请选择对应 Minecraft 版本和加载器。

| Minecraft | Loader / 加载器 | File / 文件 | Java |
| --- | --- | --- | --- |
| 1.21.1 | Fabric (main / 主版本) | `orients_hotpot_chronicles-1.21.1-fabric-0.3.2-alpha.jar` | 21+ |
| 1.21.1 | NeoForge | `orients_hotpot_chronicles-1.21.1-neoforge-0.3.2-alpha.jar` | 21+ |
| 26.3 | Fabric | `orients_hotpot_chronicles-26.3-fabric-0.3.2-alpha.jar` | 25+ |
| 26.3 | NeoForge | `orients_hotpot_chronicles-26.3-neoforge-0.3.2-alpha.jar` | 25+ |
| 26.2 | Fabric | `orients_hotpot_chronicles-26.2-fabric-0.3.2-alpha.jar` | 25+ |
| 26.2 | NeoForge | `orients_hotpot_chronicles-26.2-neoforge-0.3.2-alpha.jar` | 25+ |
| 26.1 – 26.1.2 | Fabric | `orients_hotpot_chronicles-26.1-fabric-0.3.2-alpha.jar` | 25+ |
| 26.1.2 | NeoForge | `orients_hotpot_chronicles-26.1-neoforge-0.3.2-alpha.jar` | 25+ |
| 1.20.1 | Fabric | `orients_hotpot_chronicles-1.20.1-fabric-0.3.2-alpha.jar` | 17+ |
| 1.20.1 | Forge | `orients_hotpot_chronicles-1.20.1-forge-0.3.2-alpha.jar` | 17+ |

Fabric builds need Fabric API for the same Minecraft version; Forge and NeoForge builds do not.

Fabric 版需要同 Minecraft 版本的 Fabric API；Forge 和 NeoForge 版不需要。

## Release files · 发行文件

| File / 文件 | Bytes / 字节 | SHA-256 |
| --- | ---: | --- |
| `orients_hotpot_chronicles-26.2-fabric-0.3.2-alpha.jar` | 3,463,799 | `793f1338de8a1945bfa6a9c4f35e9e0c77a83f9437198fb17090b1b92253db23` |
| `orients_hotpot_chronicles-26.2-neoforge-0.3.2-alpha.jar` | 3,337,852 | `0c353ef371b2fd04d1d4a3d8b2b4e08a40cf790b5f64d8781120e5a526f35199` |

## Build · 构建

This directory is an independent Gradle project: Fabric Loom + ModDevGradle; JDK 25 toolchain (run Gradle with JDK 21). Run `.\gradlew.bat build` (Windows) or `./gradlew build` (Linux/macOS) here; the JARs appear in each loader's `build/libs`.

本目录是独立 Gradle 工程：Fabric Loom + ModDevGradle；Java 工具链 JDK 25（用 JDK 21 运行 Gradle）。在本目录运行 `.\gradlew.bat build`（Windows）或 `./gradlew build`（Linux/macOS），JAR 在各加载器目录的 `build/libs` 里。

## License · 许可证

Code / 源代码：[MIT](LICENSE)。Original non-code assets / 原创非代码资源：[CC BY-NC-SA 4.0](ASSETS_LICENSE)。Minecraft 原版及第三方素材遵循原权利人的条款。

Author / 作者：蘧白川 / Orient · [mryt233](https://github.com/mryt233)
