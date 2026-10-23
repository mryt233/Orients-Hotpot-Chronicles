# Orient's Hotpot Chronicles · 之晗的火锅物语

**Minecraft 1.21.1 · NeoForge · 0.1.1-alpha**

[Full guide / 完整玩法](https://github.com/mryt233/Orients-Hotpot-Chronicles#readme) · [Download / 下载](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.21.1-neoforge) · [Report an issue / 反馈问题](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)

> **Alpha / 测试版**
> Only preliminary testing has been performed. Compatibility with other mods and potential bugs have not been thoroughly verified. Back up your saves and try this version in a new world first. Bug reports are welcome.
>
> 目前仅做过初步测试，与其他模组的兼容性及潜在 bug 尚未充分验证。请备份存档，优先在新存档中试玩，欢迎反馈问题。

## English

A NeoForge port of the published **0.1.1-alpha**: brick stoves, iron and copper nine-grid hotpots, four broths, bamboo chopsticks, hotpot tables, stools, benches, plates, tripe, bowls and drinks. Drinks are Creative-only in this version. The later dipping-sauce stations, cutting boards and food-preparation additions are outside this release.

### Requirements

| Item | Requirement | Recorded test version |
| --- | --- | --- |
| Minecraft | **1.21.1 only** | 1.21.1 |
| Loader | **NeoForge 21.1.264+** | **21.1.264** |
| Java | **21+** | Java 21 |
| Installation | **Both client and server** | — |
| Extra API dependency | Fabric API is not required for this build | — |

### Installation

1. Install [NeoForge](https://neoforged.net/) for Minecraft **1.21.1**. The recorded tests used **21.1.264**.
2. Put `orients_hotpot_chronicles-1.21.1-neoforge-0.1.1-alpha.jar` into the `mods` folder on both client and server.
3. Select the Minecraft **1.21.1 NeoForge** profile and launch with **Java 21 or newer**.

This is the NeoForge build for **Minecraft 1.21.1**. Support for other Minecraft versions or Forge loaders has not been established.

### Preliminary tests and known differences

The existing port reports record a successful build, client startup to the main menu, and **15/15 command-driven checks** in both the development and official dedicated-server environments. The checks cover selected block placements, campfire cooking, a dropped pot retaining its contents, and tripe drops.

Player-operated interactions, screens, visual effects, multiplayer and other-mod combinations still need broader testing. Registration of custom renderers does not establish that every visual detail is correct.

Compared with the published Fabric 0.1.1-alpha builds:

- Tall hotpot tables made from flammable woods can burn; crimson and warped variants remain nonflammable.
- Cow and mooshroom drops may still receive tripe when a data pack replaces their loot tables.

## 中文

本版是已发布 **0.1.1-alpha** 的 **Minecraft 1.21.1 NeoForge 移植版**，包含灶台、铁锅 / 铜锅、四种汤底、竹筷、火锅桌、桌凳、盘子、毛肚、碗与饮料。饮料在本版中仍只能通过创造模式获取，后续开发的蘸料台、砧板和切菜等内容不属于本版。

### 运行要求

- 仅支持 **Minecraft 1.21.1**，使用 **NeoForge 21.1.264+**（现有测试版本：**21.1.264**）。
- 使用 **Java 21 或更新版本**。
- **客户端与服务端均需安装**，本版**不需要 Fabric API**。

### 安装

1. 安装 Minecraft **1.21.1** 对应的 [NeoForge](https://neoforged.net/)；建议先使用已记录测试的 **21.1.264**。
2. 将 `orients_hotpot_chronicles-1.21.1-neoforge-0.1.1-alpha.jar` 放入客户端和服务端的 `mods` 文件夹。
3. 选择 Minecraft **1.21.1 NeoForge** 配置，用 **Java 21+** 启动。

本文件是 **Minecraft 1.21.1 的 NeoForge 版**，没有据此确认其他 Minecraft 版本或 Forge 加载器的兼容性。

### 初步测试与已知差异

现有移植报告记录了构建成功、客户端启动至主菜单，以及开发环境和官方正式服务端各 **15/15 项命令检查通过**。检查覆盖部分方块摆放、篝火烹饪、挖锅保留内容、毛肚掉落。

右键交互、菜单操作、实际渲染、多人联机和其他模组组合仍需进一步实测。渲染器已注册不代表所有视觉细节都已验收。

相较已发布的 Fabric 0.1.1-alpha：

- 可燃木材的高火锅桌现在会燃烧，绯红与诡异木版本仍不可燃。
- 数据包替换牛 / 哞菇掉落表后，本版仍可能额外加入毛肚掉落。

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

## Release file · 发行文件

`orients_hotpot_chronicles-1.21.1-neoforge-0.1.1-alpha.jar`

SHA-256: `71342e8e533a6c2df21465b0adc490cb54457b52f26913de818b8fe54b68e663`

## Build · 构建

This directory is an independent Architectury Loom project. Use **JDK 21** and run `./gradlew build` on Linux/macOS, or `.\gradlew.bat build` on Windows. Initial builds need dependency downloads.

本目录为独立 Architectury Loom 工程，使用 **JDK 21**，在本目录运行 Windows 的 `.\gradlew.bat build` 或 Linux/macOS 的 `./gradlew build`。首次构建需要下载依赖。公开源码不固定开发机器的 JDK 路径或代理；本地开发环境按移植手册继续使用已配置的固定代理。

## License · 许可证

Code / 源代码：[MIT](LICENSE)。Original non-code assets / 原创非代码资源：[CC BY-NC-SA 4.0](ASSETS_LICENSE)。两类内容分别受对应许可约束，Minecraft 原版及第三方素材遵循原权利人的条款。

Author / 作者：蘧白川 / Orient · [mryt233](https://github.com/mryt233)
