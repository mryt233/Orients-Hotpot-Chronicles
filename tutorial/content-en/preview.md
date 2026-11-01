<!-- 作者：mryt233；2026-10-30 按简体原文同步 #72–#82，仅开发版。mryt233 改：2026-11-01 补 #87–#98。 -->
# Development Preview

:::dev
These features are **in development after 0.1.4-alpha (development build r132, preview build 0.2.0-alpha)** and have not been released. They are not in the released build. Names, values, and controls may change before release.
:::

## Rendering fix {#render-fix}

- When a pot heats up after lighting, the boiling surface and bubbles no longer jump from "very fast" to "slower"; bubble speed now follows the heat smoothly and matches the released build once stable.<!--清单#87-->

## Minced pork and ham sausages {#sausage}

{{icons:minced_pork,raw_ham_sausage,ham_sausage,ham_sausage_slice,cooked_ham_sausage_slice}}

1. Place a vanilla **raw porkchop** on the cutting board and **right-click once with the Iron Kitchen Knife** to get **1 Minced Pork**; a stonecutter also works. Pounding pork meatballs with a rolling pin is unchanged.
2. Shapelessly combine **Minced Pork + 1 vanilla wheat** to get **3 Raw Ham Sausages**. No separate starch item is needed.<!--清单#72-->
3. Heat a raw sausage in a hotpot, furnace, smoker, or campfire to make a **Ham Sausage**, restoring **3 hunger points** by default. Earlier development builds called it “Cooked Ham Sausage”; its name and ID now use “Ham Sausage”.<!--清单#76-->
4. **Cut a Ham Sausage once with the knife on the board** to get **3 Ham Sausage Slices**; a stonecutter works too. **Raw Ham Sausages cannot be placed on the board for slicing.** Slices can be eaten directly for **1 hunger point** by default or placed in bowls and plates.<!--清单#75-->
5. **Slices can be heated once more** in a hotpot, furnace, smoker, or campfire to make **Cooked Ham Sausage Slices**, restoring **2 hunger points** by default. Cooked slices cannot go back into the pot.<!--清单#77-->

{{recipe:minced_pork,raw_ham_sausage,ham_sausage,ham_sausage_slice,cooked_ham_sausage_slice}}

:::tip
Remember the chain: **raw porkchop → Minced Pork → Raw Ham Sausage → Ham Sausage → Ham Sausage Slice → Cooked Ham Sausage Slice**. Cook the whole sausage before slicing it; the slices can then be heated again. The earlier development note saying slices cannot enter the pot is no longer current.
:::

## Eating when full {#chopsticks}

{{icons:bamboo_chopsticks,minecraft:bowl,quartz_small_dish}}

- Food held with chopsticks **can be eaten even with a full hunger bar**: the hunger bar cannot rise further, but **saturation is still added**, up to 20.
- **All of this mod's foods can be eaten directly from your hand while full** (raw and cooked ingredients, soup bowls, tripe, scallions, cilantro, and so on); again the hunger bar stays full while saturation is added up to 20. **Always Eat** (which lets other mods' foods be eaten while full) does not conflict.<!--清单#99-->
- **Right-clicking a bowl or plate while holding cooked food still eats it.** Returning food now uses **sneak + right-click**, rather than returning it automatically when full.
- Sneak + right-click still rests chopsticks on a bowl or plate, and an empty-hand right-click retrieves them. **Sneak + right-click returns food first** when the chopsticks hold any (the chopsticks stay in hand); **only empty chopsticks rest on the rim**, so the two actions no longer conflict. Normal right-click eating is unchanged.<!--清单#74--><!--清单#84-->

## Configurable values for administrators {#config}

{{icons:hotpot_guide}}

- The game creates `config/orients_hotpot_chronicles.json`. Advanced players can adjust food nutrition, broth speed, fuel, seasonings, and other values. An administrator runs **`/orientshotpot reload`** to apply changes immediately without restarting. On a multiplayer server, edit only the server file; values synchronize on joining and reloading. Change recipes with a data pack, followed by vanilla `/reload`.<!--清单#78-->
- **Food hunger and saturation also reload immediately.** Eating directly, with chopsticks, and in the hotpot screen uses the same nutrition values. Missing or invalid entries individually fall back to defaults with warnings. If the whole file is malformed, all values use defaults and **the original broken file is preserved** for repair. The command reports the warning count.<!--清单#81-->

:::tip
Values in this guide are defaults. If a server changes the configuration, the server's values apply. The configuration explains each field's meaning, range, and units.
:::

## Resilience and internal changes {#maintenance}

{{icons:cutting_board,iron_kitchen_knife}}

- **Cutting-board fallback:** if a modpack removes an ingredient's stonecutting recipe, the board still uses its built-in cuts. Data-pack recipes take priority when present.<!--清单#73-->
- **Addon hooks:** extension points are ready for other addons; gameplay is unchanged without an addon.<!--清单#79-->
- **Compatibility cleanup:** third-party integrations load separately. A failing integration is skipped without preventing other integrations from loading; player controls are unchanged.<!--清单#80-->
- **Translated tag names:** recipe viewers can show readable names for cuttable ingredients, ingredients cut piece by piece, heat sources, and tabletop decorations instead of raw tag IDs.<!--清单#82-->
- **Compatibility wiring fix:** third-party broth sources and custom fuels are now recognized by the actual cooking logic instead of only being recorded in the compatibility layer's own tables. Gameplay is unchanged without third-party mods. Specific integrations depend on what is actually installed; this guide makes no advance promises.<!--清单#85-->
- **Chopstick internal fix:** picking up food with chopsticks no longer throws an exception on the NeoForge side, and chopsticks already holding food in old worlds still read back correctly. This is a development-build fix; no official NeoForge release includes it yet.<!--清单#86-->
- **Long-distance teleport crash fix:** after respawning or being teleported to distant chunks, the client no longer crashes because of a stove left in an old chunk, and unloaded pots are no longer ticked by the client.<!--清单#88-->
- **Server freeze fix:** servers with Tall Brick Stoves or Sauce Stations no longer freeze entirely when players join, teleport, or load new chunks.<!--清单#89-->
- **Firebox fuel no longer disappears:** fuel placed in the firebox while the stove has no pot is still there after a restart; the stove and Sauce Station screens now close when you walk away or the block is broken.<!--清单#90-->
- **Correct cooking time after a restart:** uncooked food left in a pot continues with its proper cooking time after a server restart.<!--清单#91-->
- **Smoother pot animation:** the food shown in a rotating slot no longer suddenly switches when the fire is lit or goes out, and bubbles stay smooth at high frame rates.<!--清单#92-->
- **Campfire pot heat display:** when a pot cooks over a campfire, its screen shows that it is heating and how many seconds remain, instead of "no fire".<!--清单#93-->
- **Breaking a pot on a tall stove:** mining the pot from the upper half of a Tall Brick Stove removes only the pot.<!--清单#94-->
- **Hoppers work with pots:** a hopper above a pot only inserts raw ingredients that can be cooked, and a hopper below only pulls out cooked food.<!--清单#95-->
- **Adjustable drink nutrition:** server owners can set how much hunger each sip of a drink restores in the configuration file; the reload command's message now follows the player's language.<!--清单#96-->
- **Internal cleanup:** pots compute and synchronize more efficiently; gameplay is unchanged.<!--清单#97-->
- **More natural food rotation in the pot:** when several foods share a slot, they keep the same size while rotating; the current one sinks into the broth as the next one floats up.<!--清单#98-->
- **Framework maintenance:** when a server owner reloads the configuration, each warning is shown in their own language and explains the problem. On Fabric servers, a client whose mod versions do not match the server is told to install the same versions when joining, instead of joining with features silently not working.<!--清单#100-->
- **Translation review:** ham sausage names in several languages are more accurate, and the zh_tw guide reads more naturally, with vanilla names matching the official zh_tw translations.<!--清单#101-->
- **Loader wiring progress (development only):** the 1.21.1 main project now runs its development client on both Fabric and NeoForge (pot, firebox, sauce screens, the Hotpot Guide, rendering, and configuration sync). This is development progress only and **does not mean a new download is available**; the official support matrix on the downloads page remains 0.1.4-alpha (1.21.1 Fabric) and 0.1.3-alpha-hotfix1 (1.21.1 NeoForge, 1.20.1).<!--清单#83-->
