<!-- 作者：；英文翻译。 -->
# Downloads, Installation & Feedback

## Supported versions {#versions}

The mod supports **two Minecraft versions and four loader combinations**, all currently on **0.1.2-alpha**:

| Minecraft | Loader | Java | Also required |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric** (used in this guide) | 21 or later | Fabric Loader 0.15.11+ and **Fabric API** (0.116.14+1.21.1) |
| 1.21.1 | NeoForge | 21 or later | NeoForge 21.1.264 or later (**no** Fabric API required) |
| 1.20.1 | Fabric | 17 or later | Fabric Loader 0.15.11+ and **Fabric API** (0.92.12+1.20.1) |
| 1.20.1 | Forge | 17 or later | Forge 47.1+ (tested with 47.4.28; **no** Fabric API required) |

:::tip
**This guide demonstrates 1.21.1 Fabric.** Gameplay, recipes, and values are the same on the other three builds, so you can follow the same screens and controls. A few small differences are listed under [How the ports differ](#ports).
:::

- The same 1.20.1 Forge jar has also had preliminary server checks on NeoForge 1.20.1 (47.1.106). Client and multiplayer behavior there have not been verified.
- An older 0.1.1-alpha build exists for **1.20.4 Fabric**. It is no longer updated and is outside this guide's scope.
- Install the same jar on both the client and server for your chosen version.

## Where to download {#download}

| Source | Link | Notes |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | Available normally |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **If the page does not open, it is still awaiting approval.** Use another source for now. |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | One release page per Minecraft version, containing both Fabric and Forge / NeoForge jars |

The two GitHub release pages:

- [0.1.2-alpha · Minecraft 1.21.1 · Fabric + NeoForge](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.2-alpha-mc1.21.1)
- [0.1.2-alpha · Minecraft 1.20.1 · Fabric + Forge](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.2-alpha-mc1.20.1)

**Check both the Minecraft version and the loader before downloading.** They appear in the filename, for example `orients_hotpot_chronicles-1.21.1-fabric-0.1.2-alpha.jar`. Choosing the wrong version or loader will stop the game from launching.

## Installation {#install}

### Fabric (1.21.1 / 1.20.1)

1. Use [Fabric Loader](https://fabricmc.net/use/) to create a game profile for the matching Minecraft version.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for **that same Minecraft version**.
3. Put both Fabric API and this mod's jar in `.minecraft/mods`, then launch the game.

### NeoForge (1.21.1)

1. Install [NeoForge](https://neoforged.net/) 21.1.264 or later.
2. Put this mod's NeoForge jar in `mods` and launch. **Fabric API is not required.**

### Forge (1.20.1)

1. Install [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) for 1.20.1 (47.1+; tested with 47.4.28).
2. Put this mod's Forge jar in `mods` and launch. **Fabric API is not required.**

In Creative mode, look for the **Orient's Hotpot Chronicles** tab. The smaller tabs on the left sort items into kitchenware, tableware, tables, seating, and food.

:::warn
The mod is still in **alpha** and has **only had preliminary testing**. Compatibility with other mods, multiplayer, and unusual interactions may have bugs.
Worlds from 0.1.0-alpha.1 may lose modded items or show changed appearances after upgrading. **Back up your world first; a new world is recommended.**
:::

## How the ports differ {#ports}

The 1.20.1 Fabric / Forge and 1.21.1 NeoForge builds are ports of 1.21.1 Fabric. **Gameplay, recipes, values, and textures are the same.** The visible differences are small:

| Feature | 1.21.1 Fabric | 1.20.1 |
| --- | --- | --- |
| Steam above the pot | White smoke particles | Cloud particles (also white and rising, slightly larger) |
| Returning bowls after foods such as mushroom stew | Reads the food's own properties | Follows vanilla bowl-return rules |

Keep in mind: **new content usually arrives on Fabric 1.21.1 first, with ports following later.** Release pages and the version tags in this guide explain what is available.

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
