# Version ports · 版本与加载器

Minecraft 1.21.1 (Fabric + NeoForge) is the repository-root project. The directories below are the ports of **0.2.1-alpha**; each Minecraft version is one independent multi-loader Gradle project (framework + common + one directory per loader). All files of one mod version share one Release.

Minecraft 1.21.1（Fabric + NeoForge）位于仓库根目录；下列目录是 **0.2.1-alpha** 的移植版，每个 Minecraft 版本是一个独立的多加载器 Gradle 工程（前置框架 + 公共代码 + 每个加载器一个目录）。同一模组版本的所有文件集中在一个发布页。

| Minecraft | Loader / 加载器 | Mod / 模组 | Java | Source / 源码 | Download / 下载 |
| --- | --- | --- | --- | --- | --- |
| 26.2 | Fabric, NeoForge | 0.2.1-alpha | 25+ | [26.2/README.md](26.2/README.md) | [Release](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.2.1-alpha) |
| 26.1, 26.1.1, 26.1.2 (NeoForge: 26.1.2 only) | Fabric, NeoForge | 0.2.1-alpha | 25+ | [26.1/README.md](26.1/README.md) | [Release](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.2.1-alpha) |
| 1.20.1 | Fabric, Forge | 0.2.1-alpha | 17+ | [1.20.1/README.md](1.20.1/README.md) | [Release](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.2.1-alpha) |

Fabric builds need Fabric API for the same Minecraft version; Forge / NeoForge builds do not. Install on both client and server. Requirements, test scope and SHA-256 are in each project's README.

Fabric 版需要同 Minecraft 版本的 Fabric API，Forge / NeoForge 版不需要。客户端和服务端都要安装。运行要求、测试范围和 SHA-256 见各工程的 README。

## Legacy ports · 旧移植版（已停止维护 / discontinued）

| Directory / 目录 | Minecraft | Loader / 加载器 | Last version / 最后版本 |
| --- | --- | --- | --- |
| `1.21.1/neoforge/` | 1.21.1 | NeoForge | 0.1.4-alpha（replaced by the root project / 已由仓库根目录工程取代） |
| `1.20.4/fabric/` | 1.20.4 | Fabric | 0.1.1-alpha |

The old single-loader 1.20.1 sources (0.1.4-alpha) are kept in the history and tags `v0.1.4-alpha` and earlier. Old releases and download links are unchanged.

旧的单加载器 1.20.1 源码（0.1.4-alpha）保留在历史和 `v0.1.4-alpha` 及更早的标签里。旧发布页和下载链接不变。
