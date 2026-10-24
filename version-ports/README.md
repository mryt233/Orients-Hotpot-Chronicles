<!-- 。 -->
# Version ports · 版本与加载器

Minecraft 1.21.1 Fabric is the repository-root project. Each port below is an independent Gradle project. Builds for the same mod version and Minecraft version share one Release, with a separate JAR for each loader.

Minecraft 1.21.1 Fabric 位于仓库根目录；下列各移植目录是独立 Gradle 工程。同一模组版本、同一 MC 版本合并为一条 Release，各加载器使用独立 JAR。

| Minecraft | Loader / 加载器 | Mod / 模组 | Java | Source / 源码 | Download / 下载 |
| --- | --- | --- | --- | --- | --- |
| 1.21.1 | fabric | 0.1.2-alpha | 21+ | [README](../README.md) | [Release](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.2-alpha) |
| 1.20.1 | fabric | 0.1.2-alpha | 17+ | [README](1.20.1/fabric/README.md) | [Release](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.2-alpha) |
| 1.20.1 | forge | 0.1.2-alpha | 17+ | [README](1.20.1/forge/README.md) | [Release](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.2-alpha) |
| 1.21.1 | neoforge | 0.1.2-alpha | 21+ | [README](1.21.1/neoforge/README.md) | [Release](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.2-alpha) |
| 1.20.4 | fabric | 0.1.1-alpha | 17+ | [README](1.20.4/fabric/README.md) | [Release](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.20.4-fabric) |

Fabric requires the matching Fabric API; Forge / NeoForge do not. Both client and server need the mod. Only preliminary testing has been performed; player interactions, multiplayer and other-mod compatibility still need testing.

Fabric 需要同 MC 版本 Fabric API，Forge / NeoForge 不需要。客户端和服务端都要安装。目前仅做过初步测试，实际交互、多人联机及其他模组兼容性仍需实测。1.20.4 尚未同步新版内容；旧标签与发布内容保持不变。
