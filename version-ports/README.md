<!-- ；已发布版本索引。 -->
# Version ports · 版本与加载器移植

Each `<Minecraft>/<loader>/` directory is an independent Gradle project based on the published **0.1.1-alpha**. Minecraft 1.21.1 Fabric remains at the repository root.

每个 `<Minecraft版本>/<加载器>/` 目录均为独立 Gradle 工程，以已发布 **0.1.1-alpha** 为底本；Minecraft 1.21.1 Fabric 工程仍位于仓库根目录。

| Minecraft | Loader / 加载器 | Java | Source / 源码 | Release status / 发布状态 |
| --- | --- | --- | --- | --- |
| 1.20.1 | Fabric | 17+ | [1.20.1/fabric](1.20.1/fabric/README.md) | [0.1.1-alpha](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.20.1-fabric) |
| 1.20.4 | Fabric | 17+ | [1.20.4/fabric](1.20.4/fabric/README.md) | [0.1.1-alpha](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.20.4-fabric) |
| 1.20.1 | Forge | 17+ | [1.20.1/forge](1.20.1/forge/README.md) | [下载 / Download](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.20.1-forge) |
| 1.21.1 | NeoForge | 21+ | [1.21.1/neoforge](1.21.1/neoforge/README.md) | [下载 / Download](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.1-alpha-mc1.21.1-neoforge) |

> Only preliminary testing has been performed. Compatibility with other mods and potential bugs have not been thoroughly verified. Back up your saves and try a new world first.
>
> 目前仅做过初步测试，与其他模组的兼容性及潜在 bug 尚未充分验证。请备份存档，优先在新存档中试玩。

Forge 1.20.1 and NeoForge 1.21.1 do not require Fabric API. The Forge JAR also passed recorded smoke checks on a NeoForge 1.20.1 / 47.1.106 dedicated server; this is not full client or modpack compatibility verification. See each README for requirements, test scope and known differences.

Forge 1.20.1 与 NeoForge 1.21.1 不需要 Fabric API。Forge JAR 另在 NeoForge 1.20.1 / 47.1.106 正式服务端通过初步检查，不代表客户端或整合包兼容性已全部验证。版本要求、测试范围与已知差异见各自 README。

Existing release tags keep their original directory layout and content. / 既有发布标签保留原目录和内容。

Code / 源代码：MIT；original non-code assets / 原创非代码资源：CC BY-NC-SA 4.0。
