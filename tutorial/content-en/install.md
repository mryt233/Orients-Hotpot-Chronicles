<!-- 作者：mryt233；英文翻译，原文：mryt233。 -->
<!-- mryt233 改（2026-11-22）：全量按 0.3.1-alpha 五版本十包同步发布口径更新英文安装说明，含 26.3。 -->
# Downloads, Installation & Feedback

## Supported versions {#versions}

The latest release **0.3.1-alpha** simultaneously supports **5 Minecraft version lines (10 packages): 1.21.1, 1.20.1, 26.1.x, 26.2, and 26.3**. Every JAR **bundles `orient_framework 0.2.2`**, so just drop the matching single JAR into `mods`:

| Minecraft | Loader | Java | Also required |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric** (used in this guide, **0.3.1-alpha**) | 21 or newer | Fabric Loader 0.15.11+, matching **Fabric API** (tested with 0.101.2+1.21.1 / 0.116.14+1.21.1) |
| **1.21.1** | **NeoForge** (**0.3.1-alpha**) | 21 or newer | NeoForge 21.1.186 or newer |
| **1.20.1** | **Fabric** (**0.3.1-alpha**) | 17 or newer | Fabric Loader 0.14.21+, **Fabric API** 0.89.0+ for 1.20.1 (tested with 0.89.0 and 0.92.12) |
| **1.20.1** | **Forge** (**0.3.1-alpha**) | 17 or newer | Forge 47.0.0+ (47.0.0 dedicated server; 47.4.28 on both sides) |
| **26.1 / 26.1.1 / 26.1.2** | **Fabric** (**0.3.1-alpha**) | 25 or newer | Fabric Loader 0.19.5+ and **Fabric API** 0.145.0+ for your 26.1.x patch |
| **26.1.2** | **NeoForge** (**0.3.1-alpha**) | 25 or newer | NeoForge 26.1.2.71 or newer |
| **26.2** | **Fabric / NeoForge** (**0.3.1-alpha**) | 25 or newer | Fabric Loader 0.19.5+ and **Fabric API** 0.152.0+ for 26.2, or NeoForge 26.2.0.57+ |
| **26.3** | **Fabric / NeoForge** (**0.3.1-alpha**) | 25 or newer | Fabric Loader 0.19.5+ and **Fabric API** 0.160.3+ for 26.3, or NeoForge 26.3.0.76+ |

:::tip
**This guide demonstrates 1.21.1 Fabric.** Starting in 0.3.1-alpha, gameplay, ingredients, crafting, minigames, swappable tables/racks, config options, and open registration stay synchronized across all five version lines. Minor vanilla-layer differences are listed under [Differences between versions](#ports) below.
:::

- **Automatic world & config compatibility**:
  - Upgrading from `0.2.2-alpha` / `0.3.0-alpha` automatically and losslessly migrates **all 40 legacy preset hotpot tables** (both placed blocks when chunks load and items in player inventories on login, plus chested legacy tables when placed) into universal hotpot tables while keeping their original appearance — no purple-and-black blocks or lost saves;
  - Broth, seasoning, and removed dry-goods soaking config keys (`soaked_*` / `dry_goods_soaking`) are silently migrated and cleaned up on load with zero reload warnings; backing up worlds before upgrading is still recommended.
- **1.20.4 Fabric** remains on legacy 0.1.1-alpha and is no longer maintained.
- **Multiplayer requirement**: because `0.3.1-alpha` syncs gameplay mode flags, custom rhythm track datapacks, and string broth/seasoning IDs over the network, **clients and servers must both run `0.3.1-alpha`** — do not mix versions.

## Where to download {#download}

| Source | URL | Notes |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | Available |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **If unavailable, it is under review** — use another source |
| **MCMOD** | <https://www.mcmod.cn/class/32185.html> | Chinese mod encyclopedia: overview and community discussion |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | One page per release with all loader JARs |

**Check two details before downloading: Minecraft version and mod loader.** Both are shown in the filename, e.g. `orients_hotpot_chronicles-1.21.1-fabric-0.3.1-alpha.jar`, `orients_hotpot_chronicles-1.21.1-neoforge-0.3.1-alpha.jar`, or `orients_hotpot_chronicles-26.3-fabric-0.3.1-alpha.jar`. The wrong build will fail to load.

## How to install {#install}

### Fabric (1.21.1 / 1.20.1 / 26.1.x / 26.2 / 26.3)

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for the **same** Minecraft version.
3. Put both Fabric API and this mod's JAR into `.minecraft/mods` and launch the game. (Install **Mod Menu** as well if you want an in-game Config button.)

### NeoForge (1.21.1 / 26.1.x / 26.2 / 26.3)

1. Install [NeoForge](https://neoforged.net/) for your Minecraft version (21.1.186+ on 1.21.1, or the corresponding NeoForge build on 26.x).
2. Put this mod's NeoForge JAR into `mods` and launch the game. **Fabric API is not needed.**

### Forge (1.20.1)

1. Install [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) 1.20.1 (47.0.0+, tested on 47.0.0 and 47.4.28).
2. Put this mod's Forge JAR into `mods` and launch the game. **Fabric API is not needed.**

Once installed, open the Creative inventory tab **Orient's Hotpot Chronicles**, with left-side filter subtabs (All / Kitchenwares / Tableware / Tables & Seats / Food); open the mod config screen anytime to switch between **Full Mode** and **Simple Mode**.

:::warn
This mod is still in **alpha**. Upgrading worlds from very early builds like `0.1.0-alpha.1` may lose or alter blocks, so **back up saves first** and prefer a fresh world when upgrading across major eras.
:::

## Differences between versions {#ports}

`0.3.1-alpha` aligns gameplay and content across **1.21.1, 1.20.1, 26.1.x, 26.2, and 26.3**, with only tiny vanilla-engine differences:

| Item | 1.21.1 / 26.1.x / 26.2 / 26.3 | 1.20.1 (Fabric / Forge) |
| --- | --- | --- |
| Steam particles from the pot | White smoke particles | Cloud particles (white rising puffs, slightly larger) |
| Bowl return after eating | Uses food item properties | Uses vanilla and mod return rules |

## Feedback and contact {#feedback}

Found a bug, or have a question or suggestion? Reach the author here:

| Channel | Contact |
| --- | --- |
| **GitHub Issues** | [Open an issue](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose) (bugs, suggestions, questions) |
| **QQ Group** | **248314791** (no reply usually means the author is asleep) |
| **Bilibili** | 蘧白川 |
| **Email** | orientcheung233@gmail.com |
| Repository | <https://github.com/mryt233/Orients-Hotpot-Chronicles> |

**When reporting a bug, please include:**
1. Minecraft version, loader (Fabric / Forge / NeoForge), and mod version;
2. What you were doing and which step went wrong;
3. Screenshots if possible, plus `latest.log` or the crash report for crashes.

Author: **蘧白川 / Orient** (Chinese and English names of the same author). Code is licensed under MIT; original textures and models are CC BY-NC-SA 4.0.
