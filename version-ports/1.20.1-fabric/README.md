# Orient's Hotpot Chronicles · 之晗的火锅物语

**Fabric · Minecraft 1.20.1 · 0.1.1-alpha**

[Download / 下载](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.20.1-fabric) · [Full gameplay guide / 完整玩法](https://github.com/mryt233/Orients-Hotpot-Chronicles#readme) · [Report an issue / 反馈问题](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)

> **Alpha / 测试版**
> Only preliminary testing has been performed. Compatibility with other mods and potential bugs have not been thoroughly verified. Back up your saves, try the mod in a new world first, and report any issues.
>
> 目前仅做过初步测试，与其他模组的兼容性及潜在 bug 尚未充分验证。建议备份存档，优先在新存档中试玩，欢迎反馈问题。

## Requirements · 运行要求

| Item / 项目 | Requirement / 要求 | Tested dependency / 测试时使用 |
| --- | --- | --- |
| Minecraft | **1.20.1 only / 仅 1.20.1** | 1.20.1 |
| Loader / 加载器 | **Fabric**, Loader **0.15.11+** | 0.19.3 |
| Fabric API | Use the build for **1.20.1** / 使用 **1.20.1 对应版本** | 0.92.12+1.20.1 |
| Java | **17+** | Java 17 bytecode / Java 17 字节码 |
| Installation / 安装位置 | **Both client and server / 客户端和服务端均需安装** | — |

## Installation · 安装

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.20.1**.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for **1.20.1**.
3. Download `orients_hotpot_chronicles-1.20.1-fabric-0.1.1-alpha.jar` and put it in the `mods` folder on both client and server.

1. 安装 Minecraft **1.20.1** 对应的 Fabric Loader。
2. 安装 **1.20.1** 对应的 Fabric API。
3. 下载 `orients_hotpot_chronicles-1.20.1-fabric-0.1.1-alpha.jar`，放入客户端和服务端的 `mods` 文件夹。

## About this port · 移植说明

This port is based on the published **0.1.1-alpha** for Minecraft 1.21.1. It includes the stoves, hotpots, broths, chopsticks, tables, seats, plates, tripe, bowls and drinks from that release. The later dipping-sauce and food-preparation features in the development workspace are outside this version. Pot steam uses white cloud particles in 1.20.1, so it looks slightly different from 1.21.1.

本版以已发布的 Minecraft 1.21.1 **0.1.1-alpha** 为底本，包含该版的灶台、锅、汤底、筷子、桌凳、盘子、毛肚、碗与饮料。后续开发中的蘸料、切菜等内容不属于本版。1.20.1 的锅中热气使用白色云粒子，外观与 1.21.1 略有不同。

## Test status · 测试范围

Preliminary checks recorded during port development include compilation, dedicated-server startup, command-based placement/cooking/drop checks, and client startup to the main menu. Player interactions, screens, rendering and compatibility with other mods still need broader in-game testing.

移植开发记录中的初步检查包括编译、服务端启动、命令摆放 / 烹饪 / 掉落检查，以及客户端启动至主菜单。右键交互、界面、渲染及与其他模组的兼容性仍需进一步游戏内实测。

## Upcoming ports · 后续移植

Ports for Minecraft **1.20.1 Forge** and **1.21.1 NeoForge** are in progress and expected to be released soon.

Minecraft **1.20.1 Forge** 和 **1.21.1 NeoForge** 正在移植中，预计很快发布。

## Build from source · 从源码构建

This subdirectory is an independent Gradle project. Use JDK 21 to run Gradle and make JDK 17 available to the Java toolchain. Run `./gradlew build` on Linux/macOS, or `.\gradlew.bat build` on Windows, from this directory. Initial builds require network access to download dependencies.

本目录是独立 Gradle 工程。建议用 JDK 21 运行 Gradle，并安装 JDK 17 供 Java toolchain 编译；在本目录运行 Windows 的 `.\gradlew.bat build` 或 Linux/macOS 的 `./gradlew build`。首次构建需要联网下载依赖。公开配置不包含开发机器的 JDK 绝对路径。

## License · 许可证

Code / 源代码：[MIT](LICENSE)。Original non-code assets / 原创非代码资源：[CC BY-NC-SA 4.0](ASSETS_LICENSE)。两类内容分别按对应许可证授权，Minecraft 和第三方素材遵循原权利人的条款。

Author / 作者：蘧白川 / Orient · [mryt233](https://github.com/mryt233)
