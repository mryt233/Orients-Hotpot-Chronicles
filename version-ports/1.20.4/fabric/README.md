# Orient's Hotpot Chronicles · 之晗的火锅物语

**Fabric · Minecraft 1.20.4 · 0.1.1-alpha**

[Download / 下载](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.20.4-fabric) · [Full gameplay guide / 完整玩法](https://github.com/mryt233/Orients-Hotpot-Chronicles#readme) · [Report an issue / 反馈问题](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)

> **Alpha / 测试版**
> Only preliminary testing has been performed. Compatibility with other mods and potential bugs have not been thoroughly verified. Back up your saves, try the mod in a new world first, and report any issues.
>
> 目前仅做过初步测试，与其他模组的兼容性及潜在 bug 尚未充分验证。建议备份存档，优先在新存档中试玩，欢迎反馈问题。

## Requirements · 运行要求

| Item / 项目 | Requirement / 要求 | Tested dependency / 测试时使用 |
| --- | --- | --- |
| Minecraft | **1.20.4 only / 仅 1.20.4** | 1.20.4 |
| Loader / 加载器 | **Fabric**, Loader **0.15.11+** | 0.19.3 |
| Fabric API | Use the build for **1.20.4** / 使用 **1.20.4 对应版本** | 0.97.3+1.20.4 |
| Java | **17+** | Java 17 bytecode / Java 17 字节码 |
| Installation / 安装位置 | **Both client and server / 客户端和服务端均需安装** | — |

## Installation · 安装

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.20.4**.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for **1.20.4**.
3. Download `orients_hotpot_chronicles-1.20.4-fabric-0.1.1-alpha.jar` and put it in the `mods` folder on both client and server.

1. 安装 Minecraft **1.20.4** 对应的 Fabric Loader。
2. 安装 **1.20.4** 对应的 Fabric API。
3. 下载 `orients_hotpot_chronicles-1.20.4-fabric-0.1.1-alpha.jar`，放入客户端和服务端的 `mods` 文件夹。

## About this port · 移植说明

This port is based on the published **0.1.1-alpha** for Minecraft 1.21.1. It includes the stoves, hotpots, broths, chopsticks, tables, seats, plates, tripe, bowls and drinks from that release. Some items, such as the drinks, have no ingredients or crafting recipes yet and are Creative-only in this version. The later dipping-sauce and food-preparation features in the development workspace are outside this version. Pot steam uses the same white-smoke particle type as 1.21.1.

本版以已发布的 Minecraft 1.21.1 **0.1.1-alpha** 为底本，包含该版的灶台、锅、汤底、筷子、桌凳、盘子、毛肚、碗与饮料。部分物品（比如饮料）暂时没有原材料和合成配方，本版只能在创造模式物品栏获取。后续开发中的蘸料、切菜等内容不属于本版。1.20.4 的锅中热气使用与 1.21.1 相同的白烟粒子。

## Test status · 测试范围

Preliminary checks recorded during port development include compilation, dedicated-server startup, command-based placement/cooking/drop checks, and client startup to the main menu. Player interactions, screens, rendering and compatibility with other mods still need broader in-game testing.

移植开发记录中的初步检查包括编译、服务端启动、命令摆放 / 烹饪 / 掉落检查，以及客户端启动至主菜单。右键交互、界面、渲染及与其他模组的兼容性仍需进一步游戏内实测。

## Other builds · 其他版本

<!-- （2026-10-23）：各移植版都已完成，列出全部版本，替换原来的「后续移植」。 -->

Choose the JAR matching your Minecraft version and loader. All builds below are available as alpha pre-releases on the [Releases](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases) page.

请按 Minecraft 版本和加载器选择对应 JAR。下表各版本均已作为 alpha 测试版发布，可到 [Releases / 发布页](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases) 下载。

| Minecraft | Loader / 加载器 | File / 文件 | Java | Status / 状态 |
| --- | --- | --- | --- | --- |
| 1.21.1 | Fabric（main / 主版本） | `orients_hotpot_chronicles-1.21.1-fabric-0.1.1-alpha.jar` | 21+ | Published alpha / 已发布测试版 |
| 1.21.1 | NeoForge | `orients_hotpot_chronicles-1.21.1-neoforge-0.1.1-alpha.jar` | 21+ | [Published alpha / 已发布测试版](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.21.1-neoforge) |
| 1.20.4 | Fabric | `orients_hotpot_chronicles-1.20.4-fabric-0.1.1-alpha.jar` | 17+ | Published alpha / 已发布测试版 |
| 1.20.1 | Fabric | `orients_hotpot_chronicles-1.20.1-fabric-0.1.1-alpha.jar` | 17+ | Published alpha / 已发布测试版 |
| 1.20.1 | Forge | `orients_hotpot_chronicles-1.20.1-forge-0.1.1-alpha.jar` | 17+ | [Published alpha / 已发布测试版](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.20.1-forge) |

Fabric builds need [Fabric API](https://modrinth.com/mod/fabric-api) for the same Minecraft version; Forge and NeoForge builds do not.

Fabric 版需要同 Minecraft 版本的 Fabric API；Forge 和 NeoForge 版不需要。

The 1.20.1 Forge JAR also passed preliminary command checks on a NeoForge 1.20.1 / 47.1.106 dedicated server. Client, multiplayer and modpack compatibility on that loader still need testing.

1.20.1 Forge 的同一 JAR 还在 NeoForge 1.20.1 / 47.1.106 正式服务端通过了初步命令检查；该加载器下的客户端、联机和整合包兼容性仍需验证。

## Build from source · 从源码构建

This subdirectory is an independent Gradle project. Use JDK 21 to run Gradle and make JDK 17 available to the Java toolchain. Run `./gradlew build` on Linux/macOS, or `.\gradlew.bat build` on Windows, from this directory. Initial builds require network access to download dependencies.

本目录是独立 Gradle 工程。建议用 JDK 21 运行 Gradle，并安装 JDK 17 供 Java toolchain 编译；在本目录运行 Windows 的 `.\gradlew.bat build` 或 Linux/macOS 的 `./gradlew build`。首次构建需要联网下载依赖。公开配置不包含开发机器的 JDK 绝对路径。

## License · 许可证

Code / 源代码：[MIT](LICENSE)。Original non-code assets / 原创非代码资源：[CC BY-NC-SA 4.0](ASSETS_LICENSE)。两类内容分别按对应许可证授权，Minecraft 和第三方素材遵循原权利人的条款。

Author / 作者：蘧白川 / Orient · [mryt233](https://github.com/mryt233)
