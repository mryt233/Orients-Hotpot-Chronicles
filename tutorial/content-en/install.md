<!-- 作者：mryt233；英文翻译，原文：mryt233。发布前由 mryt233 同步并审核。 -->
<!-- mryt233 改，2026-11-08：同步 0.2.2-alpha 多版本发布说明；保留原译者署名。 -->
# Downloads, Installation & Feedback

## Supported versions {#versions}

The latest release, **0.2.2-alpha**, simultaneously supports **Minecraft 1.21.1, 1.20.1, 26.1.x, and 26.2**. Every jar **bundles the required `orient_framework` library**, so just put one in your `mods` folder:

| Minecraft | Loader | Java | Also required |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric** (used in this guide, **0.2.2-alpha**) | 21 or later | Fabric Loader 0.15.11+ and **Fabric API** (0.116.14+1.21.1) |
| **1.21.1** | **NeoForge** (**0.2.2-alpha**) | 21 or later | NeoForge 21.1.186 or later (**no** Fabric API required) |
| **1.20.1** | **Fabric** (**0.2.2-alpha**) | 17 or later | Fabric Loader 0.15.11+ and **Fabric API** (0.92.12+1.20.1) |
| **1.20.1** | **Forge** (**0.2.2-alpha**) | 17 or later | Forge 47.1+ (tested with 47.4.28; **no** Fabric API required) |
| **26.1.x** | **Fabric / NeoForge** (**0.2.2-alpha**) | 25 or later | Matching Fabric Loader + Fabric API, or NeoForge |
| **26.2** | **Fabric / NeoForge** (**0.2.2-alpha**) | 25 or later | Matching Fabric Loader + Fabric API, or NeoForge |

:::tip
**This guide demonstrates 1.21.1 Fabric.** In 0.2.2-alpha, all supported versions share the same gameplay, ingredients, seasonings, furniture, and configuration features; minor engine differences are listed under [How the ports differ](#ports).
:::

- **World & config compatibility:** 0.2.2-alpha splits legacy `tofu` into `cold_tofu` and `hot_tofu`. Legacy `tofu` entries in `config/orients_hotpot_chronicles.json` migrate automatically, though backing up your world before upgrading is still recommended.
- 1.20.4 Fabric remains on the old 0.1.1-alpha build and is no longer maintained.
- For multiplayer, install the matching 0.2.2-alpha jar on both the client and server.

## Where to download {#download}

| Source | Link | Notes |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | Available normally |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **If the page does not open, it is still awaiting approval.** Use another source for now. |
| **MC百科** | <https://www.mcmod.cn/class/32185.html> | Chinese mod encyclopedia: overview and discussion |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | One page per mod release, with jars for the supported loaders |

**Check both the Minecraft version and the loader before downloading.** They appear in the filename, for example `orients_hotpot_chronicles-1.21.1-fabric-0.2.2-alpha.jar` or `orients_hotpot_chronicles-1.21.1-neoforge-0.2.2-alpha.jar`. Choosing the wrong version or loader will stop the game from launching.

## Installation {#install}

### Fabric (1.21.1 / 1.20.1 / 26.1.x / 26.2)

1. Use [Fabric Loader](https://fabricmc.net/use/) to create a game profile for the matching Minecraft version.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for **that same Minecraft version**.
3. Put both Fabric API and this mod's jar in `.minecraft/mods`, then launch the game. (Install **Mod Menu** as well if you want to open the visual settings screen from the mod list.)

### NeoForge (1.21.1 / 26.1.x / 26.2)

1. Install [NeoForge](https://neoforged.net/) for your Minecraft version (21.1.186+ on 1.21.1).
2. Put this mod's NeoForge jar in `mods` and launch. **Fabric API is not required.**

### Forge (1.20.1)

1. Install [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) for 1.20.1 (47.1+; tested with 47.4.28).
2. Put this mod's Forge jar in `mods` and launch. **Fabric API is not required.**

In Creative mode, look for the **Orient's Hotpot Chronicles** tab. The smaller tabs on the left sort items into All, Kitchenware, Tableware, Furniture, and Food.

:::warn
The mod is still in **alpha** and has **only had preliminary testing**. Compatibility with other mods, multiplayer, and unusual interactions may have bugs.
Worlds from 0.1.0-alpha.1 may lose modded items or show changed appearances after upgrading. **Back up your world first; a new world is recommended.**
:::

## How the ports differ {#ports}

In 0.2.2-alpha, **1.21.1, 1.20.1, 26.1.x, and 26.2** all include the same gameplay and content, with only tiny vanilla-engine differences on 1.20.1:

| Feature | 1.21.1 / 26.1.x / 26.2 | 1.20.1 (Fabric / Forge) |
| --- | --- | --- |
| Steam above the pot | White smoke particles | Cloud particles (also white and rising, slightly larger) |
| Returning bowls after soups/stews | Reads the food's own properties | Follows vanilla and mod bowl-return rules |

## Feedback and community {#feedback}

Found a bug, have a question, or want to suggest something? Contact the author:

| Channel | Contact |
| --- | --- |
| **GitHub Issues** | [Send feedback](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose) (forms for bug reports, suggestions, and questions) |
| **QQ group** | **248314791** (if the author does not reply, they are probably asleep) |
| **Bilibili** | 蘧白川 |
| **Email** | orientcheung233@gmail.com |
| Project homepage | <https://github.com/mryt233/Orients-Hotpot-Chronicles> |

**When reporting a bug, include:**

1. Your Minecraft version, loader (Fabric / Forge / NeoForge), and mod version.
2. What you did and exactly where the problem occurred.
3. A screenshot if possible; for crashes, attach `latest.log` or the crash report.

Mod author: **蘧白川 / Orient** (Zhihan and Orient are the same person's Chinese and English names). Code is licensed under MIT; original textures, models, and other assets are licensed under CC BY-NC-SA 4.0.
