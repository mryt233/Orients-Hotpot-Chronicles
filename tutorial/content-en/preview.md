<!-- 作者：mryt233；2026-11-09 r146a 的开发中改动记在这里。 -->
# Development Preview

:::dev
Development Preview (not released yet; names and values may change). Everything added after 0.2.2-alpha is listed here.
:::

<!-- mryt233 改（2026-11-13，r148 第四批 4I～4K-2） -->
## More gameplay values and loot chances in config {#r148config}

- **Pot warmth, cold-pot nausea, and cheers are configurable**: Added 13 settings under "Pot Warmth", "Cold-Pot Nausea", and "Cheers" in the **Advanced** settings tab and `orients_hotpot_chronicles.json` (including the "blocks" unit for warmth range). Setting `warmth.duration` or `cheers.*_duration` to `0` disables that effect; changes apply right away with `/orientshotpot reload` (see [Settings & Config](faq.html#config)).<!--清单#156-->
- **Spicy broth levels are configurable**: Added 10 settings under "Spicy Broth" in the **Advanced** settings tab covering per-level duration steps, upgrade threshold, secondary bonus ticks, Fire Resistance start level and multiplier, extreme-spice Hunger bonus, and mouth-flame duration/level; default values preserve exact original behavior and sync tooltips in multiplayer.<!--清单#157-->
- **Loot drop chances and shrimp fishing weight are configurable (separate `tomato` key)**: Added 14 settings under "Loot Drop Chances" in the **Advanced** settings tab (9 grass-drop garnishes with a separate `tomato` chance, lily-pad lotus root, mushroom enoki, cow beef loin + looting bonus, and `loot.shrimp_fishing.weight`). **Run vanilla `/reload` (or restart) after editing loot chances or weight** to rebuild loot pools.<!--清单#158-->

<!-- mryt233 改（2026-11-13，r148 第四批 4D～4H） -->
## Open registration & internal refactoring: custom broths and seasonings {#r148open}

- **0.3.0 open registration summary**: Supports adding or overriding broths and seasonings via data packs (`recipe/soup/` and `recipe/seasoning/`, hot-reloaded with `/reload` and synced to multiplayer clients, with explicit config overrides taking priority) and Java add-on mods; broths without a custom bowl fill the generic {{i:soup_bowl}}`soup_bowl`, pure JSON packs tint the clear-broth base by configured soup/bubble colors, and missing entries preserve saved data as unknown placeholders (see [Adding broths and seasonings](faq.html#custom_soup_sauce)).<!--清单#154-->
- **Open-registration sample add-on & docs wrap-up (no in-game changes)**: Added a runnable multi-loader sample add-on project, documented add-on dependency ranges and tabletop renderer hooks, added the {{i:soup_bowl}} guide icon, and completed all 15 built-in sauces in the 3D viewer.<!--清单#159-->
- **Internal cleanup (no visible change)**: Broth and seasoning saves now store full IDs (e.g., `orients_hotpot_chronicles:clear`), automatically migrating legacy saves and preserving unknown extension IDs without overwriting, alongside an updated multiplayer sync format (client and server versions must match).<!--清单#153-->
- **Internal cleanup (no visible change)**: Tabletop objects (bowls, plates, drinks, bottles, chopstick rests) were unified behind open `TableItem` / `TableBlock` interfaces and client renderer callbacks so add-ons can register items without touching core placement branches; gameplay and visuals are unchanged.<!--清单#155-->

<!-- mryt233 改（2026-11-10，r146 优化第一批） -->
## Optimization pass 1: more reliable reloads and settings saves {#r146opt1}

- **Food already in the pot follows new cooking times after a reload**: After changing a cooking time and running `/orientshotpot reload` (or changing a cooking recipe in a data pack and running `/reload`), ingredients already in the pot **keep their cooking progress** and the remaining time is recalculated from the new value.<!--清单#142-->
- **Settings save failures are reported honestly**: If the config file cannot be written, the screen **stays open** and says why (the changes only apply to this session, or were not applied). If the original config file is broken, it is first backed up as `orients_hotpot_chronicles.json.broken-<time>` and the backup name is shown.<!--清单#143-->
- **Same-named ingredients from other mods no longer use this mod's cooking times**: `cooking.items.<item>` in the config only applies to this mod's own ingredients; vanilla and other mods' ingredients cook by their own recipes. The config format is unchanged.<!--清单#144-->
- **Add-on eating hooks also fire in the pot screen**: Eating cooked food with chopsticks inside the pot screen now notifies add-on mods just like eating with held chopsticks; if an add-on cancels the bite, the food stays in the pot.<!--清单#145-->
- **NeoForge mod description corrected**: The mod list no longer says the client is still being ported, and the issue link now points to the project's Issues page.<!--清单#146-->
- **The old hotpot table ID is no longer converted (very early Fabric worlds only)**: The Fabric build no longer converts the earliest `hotpot_table` into the oak hotpot table, matching NeoForge; current hotpot tables are unaffected.<!--清单#147-->
<!-- mryt233 改（2026-11-11，r147 第三批 3E）。 -->
- **Modpacks can change pot cooking and rolling-pin pounding with data packs**: Pot-only cooking (kelp, whole tofu, crispy pork, zhe'ergen slices) and rolling-pin pounding (meatballs, fish balls, minced garlic, shrimp paste, crushed peanuts, minced zhe'ergen) are now data-pack recipes of type `orients_hotpot_chronicles:pot_boiling` and `orients_hotpot_chronicles:pounding`; they can be added, changed or removed and take effect after `/reload`. Items added to the item tag `orients_hotpot_chronicles:not_pot_ingredient` can no longer go into the pot. Default gameplay is unchanged, and cooking times for this mod's own ingredients still follow the config.<!--清单#148-->
- **Recipe viewer cooking list completed and corrected**: The JEI / REI cooking list now includes the previously missing kelp, raw ham sausage, ham sausage slices, and vanilla raw meat, raw fish, potatoes and chorus fruit that can go straight into the pot; whole tofu now shows the actual 1 piece when overcooked, and re-boiling crispy pork shows the actual 0.15 XP. Only the display was corrected; actual cooking is unchanged.<!--清单#149-->
<!-- mryt233 改（2026-11-11，r147 第三批 3G）。 -->
- **Internal cleanup (no visible change)**: Right-click results and block save/load code now go through a small version-adapter module, making future ports to other Minecraft versions easier; gameplay and the save format are unchanged.<!--清单#150-->
<!-- mryt233 改（2026-11-11，r147 第三批 3H 前半）。 -->
- **Internal cleanup (no visible change)**: The guide book and settings screens were split into smaller parts by responsibility, and shared UI widgets plus one rendering lookup were merged into common helpers. Visuals, gameplay, config files and saves are unchanged.<!--清单#152-->
- **Internal cleanup (no visible change)**: The hot pot block entity was split into smaller parts by responsibility, and the embedded framework was bumped to 0.2.0 with unused legacy interfaces removed. Gameplay and the save format are unchanged.<!--清单#151-->

<!-- mryt233 改（r146f） -->
## Sauce station ingredient list: narrowed with pagination {#r146f}

- **Single-column paginated sauce station list**: the panel is narrowed and names and effects wrap across lines; turn pages with bottom arrows or the scroll wheel, page numbers are remembered during the current session, and JEI / REI avoid this area automatically.<!--清单#140-->

<!-- mryt233 改：r146e。 -->
## Chopsticks on tabletops and resting on bowls {#r146e}

- **Chopsticks tabletop placement and bowl-resting polish**: straight, parallel two-tone bamboo sticks with tips resting across a white porcelain saddle rest and ends resting on the table; four directions on the tabletop follow the player, clicking nearby finds an empty spot. Click any rim of a bowl or plate to rest them aligned to 22.5°; chopsticks holding food rest with the food when clicked on the rim, while clicking the center still places the food or dips into sauce.<!--清单#139-->

<!-- mryt233 改：r146d。 -->
## Bottle placement sounds, Author's Pick fix {#r146d}

- **Bottle placement sounds**: full and partly drunk bottles play a bottle placement sound together with a gentle liquid sound. Orient Cola uses a clearer glass sound; empty bottles only play the bottle sound. This applies to both empty and occupied table surfaces.<!--清单#137-->
- **Author's Pick fixed**: only plant protein drink unlocks this advancement, whether you take a sip from a bottle or drink a cup. Orange drink, Orient Cola, water and milk do not count.<!--清单#138-->

## Three cloth apron styles + it only covers your front {#r146b}

{{icons:cloth_apron,blue_print_cloth_apron,brown_trim_cloth_apron}}

- **Three apron styles**: plain linen (default), **blue print** (indigo with white cross flowers) and **brown trim** (linen with dark brown trim and a square pocket). Their effect, durability, enchantability and repair materials are **identical**, and all of them keep splashes off and stop mouth flames from the hottest broth.
- **Recolouring**: in a crafting grid, **any style + a dye** (shapeless): white → linen, blue → blue print, brown → brown trim. **Durability, enchantments and custom names are kept**, and it costs one dye.
- **Hangs to the knees**: the apron sits close to the body and hangs from the chest to just above the knees; the hem swings forward with your legs and lies flat when you sit; sleeves and back are transparent with only the neck strap behind, so it no longer looks like a sweater. <!--清单#141-->

## Fixed knife orientation, redesigned settings screen, and placable sesame paste / oyster sauce {#r146a}

{{icons:iron_kitchen_knife,sesame_paste,oyster_sauce}}

- **The iron kitchen knife no longer flips its orientation through the model**: the handle-at-lower-left, tip-at-upper-right, edge-facing-lower-right look is now painted straight into the texture, so the inventory, the held item, the in-game Hotpot Guide, the web tutorial and the viewer all match exactly, and re-running the texture generator can never flip it back. Note: when held in hand the blade ends up on the other side, matching what the inventory shows — this is expected. <!--清单#133-->
- **The settings screen now has Common / Advanced tabs**: opened from the mod list's Config button, it starts on **Common**, which holds the particle intensity slider plus the few things players change most (coal-grade stove heat, fallback cook time, and the heat multiplier of spicy / clear / rich-stock broth). Everything else moved to **Advanced**, grouped by cooking times, ingredients, broths, fuel, soup bowls, seasonings, drinks and food nutrition into collapsible sections that start collapsed. Each row is `- value unit +`; hold **Shift** to step by 10, click the value box to type a number directly (invalid input turns red and blocks saving); a **↺** appears on rows you changed and resets only that row; the bottom Reset button turns into "click again to confirm" before it resets everything; hovering any row shows a plain-language hint with range and default. <!--清单#134-->
- **Sesame paste and oyster sauce can be placed on the table (decoration only)**: hold one and **Shift + right-click** an empty spot on a hotpot table or tall hotpot table to put it down (it borrows a plate slot, just like chopsticks and sauce bowls), and it shows as a small 3D bottle matching its icon (a short labelled glass jar for sesame paste, a slim dark bottle for oyster sauce); **right-click empty-handed** to take it back, and mining takes only that bottle. A bottle on the table **cannot be dipped into and cannot season food** — to add this seasoning to a dish, still mix a bowl at the sauce station. <!--清单#135-->
