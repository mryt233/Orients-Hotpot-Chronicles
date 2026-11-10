<!-- 作者：mryt233；英文翻译，原文：mryt233。发布前由 mryt233 同步并审核。 -->
<!-- mryt233 改，2026-10-30：补齐 #30–#71 正式版记录；r130 单列开发版。 -->
# Changelog

This page is updated with each release, **newest first**. The Development Preview describes completed work that is not yet released. **Gameplay in this guide follows released 0.2.2-alpha.**

:::tip
**Maintenance note (players can skip this):** every game change gets a numbered entry in the unreleased section of `docs/移植同步清单.md`. The `<!--清单#N-->` markers in `preview.md` link to those entries. `python tools/build_tutorial.py` checks for omissions. At release time, move the preview into the release history and start a new preview.
:::

<!-- mryt233 改，2026-11-08：0.2.2-alpha 发布，开发版预览改名为版本号，再开一段空的预览。 -->
## Development Preview {{dev}} {#unreleased}

<!-- mryt233 改（2026-11-10，r146 优化第一批）。 -->
- **Food already in the pot follows new cooking times after a reload**: After changing a cooking time and running `/orientshotpot reload` (or changing a cooking recipe in a data pack and running `/reload`), ingredients already in the pot **keep their cooking progress** and the remaining time is recalculated from the new value.<!--清单#142-->
- **Settings save failures are reported honestly**: If the config file cannot be written, the screen **stays open** and says why (the changes only apply to this session, or were not applied). If the original config file is broken, it is first backed up as `orients_hotpot_chronicles.json.broken-<time>` and the backup name is shown.<!--清单#143-->
- **Same-named ingredients from other mods no longer use this mod's cooking times**: `cooking.items.<item>` in the config only applies to this mod's own ingredients; vanilla and other mods' ingredients cook by their own recipes. The config format is unchanged.<!--清单#144-->
- **Add-on eating hooks also fire in the pot screen**: Eating cooked food with chopsticks inside the pot screen now notifies add-on mods just like eating with held chopsticks; if an add-on cancels the bite, the food stays in the pot.<!--清单#145-->
- **NeoForge mod description corrected**: The mod list no longer says the client is still being ported, and the issue link now points to the project's Issues page.<!--清单#146-->
- **The old hotpot table ID is no longer converted (very early Fabric worlds only)**: The Fabric build no longer converts the earliest `hotpot_table` into the oak hotpot table, matching NeoForge; current hotpot tables are unaffected.<!--清单#147-->
<!-- mryt233 改：r146d。 -->
- **Bottle placement sounds**: full and partly drunk bottles play a bottle placement sound together with a gentle liquid sound. Orient Cola uses a clearer glass sound; empty bottles only play the bottle sound. This applies to both empty and occupied table surfaces.<!--清单#137-->
- **Author's Pick fixed**: only plant protein drink unlocks this advancement, whether you take a sip from a bottle or drink a cup. Orange drink, Orient Cola, water and milk do not count.<!--清单#138-->
<!-- mryt233 改：r146e。 -->
- **Chopsticks tabletop placement and bowl-resting polish**: straight, parallel two-tone bamboo sticks with tips resting across a white porcelain saddle rest and ends resting on the table; four directions on the tabletop follow the player, clicking nearby finds an empty spot. Click any rim of a bowl or plate to rest them aligned to 22.5°; chopsticks holding food rest with the food when clicked on the rim, while clicking the center still places the food or dips into sauce.<!--清单#139-->
<!-- mryt233 改（r146f） -->
- **Single-column paginated sauce station list**: the panel is narrowed and names and effects wrap across lines; turn pages with bottom arrows or the scroll wheel, page numbers are remembered during the current session, and JEI / REI avoid this area automatically.<!--清单#140-->

The next development build is being updated; new content will appear in the [Development Preview](preview.html) first.
<!-- mryt233 改，2026-11-09：r146b 布围裙三款 + 穿戴只围前面。 -->
- **Three cloth apron styles**: linen / blue print / brown trim, all with the same effect; recolour any style with a dye in a crafting grid while keeping durability, enchantments and custom names; when worn it hangs from your chest to just above the knees and the hem swings with your legs. ([Plates, Bowls & Drinks](tableware.html#apron_styles))<!--清单#136-->
<!-- mryt233 改，2026-11-09：r146a 三项（菜刀朝向、设置界面、芝麻酱蚝油上桌）。 -->
- **Fixed the iron kitchen knife orientation**: handle at lower left, tip at upper right and edge facing lower right are now painted into the texture, so inventory / held / guide / web / viewer all match and the model no longer flips; regenerating the texture cannot flip it back. ([Ingredients](ingredients.html#board))<!--清单#133-->
- **Settings screen split into Common / Advanced**: Common holds the particle slider and the few values players change most; everything else is grouped into collapsible sections. Each row has [- value unit +], Shift steps by 10, clicking the value lets you type (invalid input turns red), a per-row reset appears only when changed, Reset All asks for confirmation, and hovering shows a plain-language hint. ([FAQ](faq.html#config))<!--清单#134-->
- **Sesame paste and oyster sauce can be placed on the table (decoration only)**: Shift + right-click a table top to place it, right-click empty-handed to take it back, and mining takes only that bottle; a bottle on the table cannot be dipped into or season food — to add this seasoning, use the sauce station as before. ([Sauces](sauce.html#bottle))<!--清单#135-->

<!-- mryt233 改，2026-11-08：0.2.2-alpha 正式版发布记录（r134～r145，清单 #103～#131）。 -->
## 0.2.2-alpha{{since:0.2.2-alpha}} {#v022}

Released on 2026-11-08. **Simultaneous multi-version release for Minecraft 1.21.1 (Fabric, NeoForge), 1.20.1 (Fabric, Forge), 26.1.x (Fabric, NeoForge), and 26.2 (Fabric, NeoForge).** Every jar bundles the required `orient_framework` library—just drop the jar into `mods`. Legacy `tofu` config keys migrate automatically to `cold_tofu` and `hot_tofu`.

### New ingredients, seasonings, and cloth apron

{{icons:raw_crispy_pork,crispy_pork,boiled_crispy_pork,dried_tofu,tofu_skin,wide_noodles,sweet_potato_skin,enoki_mushroom,greens,lotus_root_slice,squid_roll,egg_fried_rice,chili_pepper,zhe_er_gen,cloth_apron}}

- **Crispy Pork** (Minced Pork + wheat + egg → {{i:raw_crispy_pork}} → {{i:crispy_pork}} → simmer again for {{i:boiled_crispy_pork}}), **Dried Tofu / Tofu Skin / Wide Potato Noodles / Sweet Potato Skin** (crafted from cold tofu or potato slices in a row or column), **Enoki Mushrooms / Bok Choy / Lotus Root & Slices / Raw Squid & Squid Rolls**, **Egg Fried Rice**, **Chili Peppers & Houttuynia Root (Zhe'ergen)** (dropped from grass, with redrawn Houttuynia and Raw Squid Roll textures; slice Houttuynia for the pot or pound into mince), plus their cooked forms. ([Ingredients & Food Preparation](ingredients.html#crispy_pork))<!--清单#116--><!--清单#123--><!--清单#127-->
- **Hot and cold tofu & overcooking into cubes:** crafting soybeans makes {{i:cold_tofu}}, simmering 10 s makes {{i:hot_tofu}}, and leaving it in the pot for 30 s breaks it into {{i:hot_tofu_cube}}; legacy `tofu` config keys migrate automatically. ([Ingredients & Food Preparation](ingredients.html#tofu))<!--清单#108--><!--清单#109-->
- **5 new seasonings (15 total) & two-column help panel:** {{i:oyster_sauce}}, {{i:sesame_paste}}, {{i:crushed_peanuts}} (pound {{i:peanut}} dropped from grass), {{i:millet_chili}}, and {{i:minced_zhe_er_gen}}; the Sauce Station `?` reference splits into two columns when over 10 seasonings; chopsticks and plates also list dipped seasonings and effects in tooltips, and 0-sip sauce bowls no longer appear. ([Dipping Sauces](sauce.html#seasoning))<!--清单#105--><!--清单#106--><!--清单#123--><!--清单#127-->
- **Cloth Apron** ({{i:cloth_apron}}): chestplate-slot apron (1 string + 6 white wool) that suppresses Level 3 Extreme Spicy mouth flames, blocks fire / campfire / lava / hot-floor splash burns within 4 blocks of a heat source, and unlocks the "Aproned Up" advancement. ([Tableware & Drinks](tableware.html#apron))<!--清单#127-->

### 4-level spicy broth, six broth flavors, and table interactions

{{icons:bamboo_chopsticks,chopstick_rest,oak_bench,orient_cola_cup,dipping_sauce}}

- **Chili peppers & 4-level Spicy Red Broth:** right-click a Spicy Red Broth section with {{i:chili_pepper}} to raise the heat level (Mild / Medium / Extra / Level 3 Extreme Spicy) or with ice / packed ice / blue ice to cool it down; scooped soup bowls and cooked dishes carry that heat level. ([Stoves & Hotpots](hotpot.html#spicy_level))<!--清单#116-->
- **Six broth flavor bonuses & effect tooltips:** cooked dishes and soup bowls grant distinct broth effects (Spicy Red: Strength + Speed + Fire Resistance at high heat; Clear: Speed + clears Nausea; Rich Stock: Resistance + Absorption with Slowness removed; Mushroom: Regeneration; Tomato: Haste + clears Nausea; Three-Delicacy: Water Breathing + Luck), with exact effects and `m:ss` durations listed in tooltips. ([Stoves & Hotpots](hotpot.html#dish_flavor))<!--清单#117--><!--清单#124--><!--清单#125-->
- **Chopsticks on the tabletop (with mini porcelain rest):** hold Shift + right-click an empty spot on a low or tall hotpot table to lay chopsticks and their held food on a white porcelain mini chopstick rest. ([Tableware & Drinks](tableware.html#chopsticks))<!--清单#119-->
- **Stackable bench ladders & smoother collision:** right-click a placed bench while holding a bench to stack up to 16 high; stacks of 2 or more work as ladders on any side while the top bench can still be sat on. Full-height bench legs now match the seat width for collision and selection so walking alongside benches stays smooth. ([Tables & Seating](tables.html#climb))<!--清单#120--><!--清单#130-->
- **Toasting (Cheers!) & octagonal beveled glass cups:** hold a filled glass cup and right-click a cup-holding player, Shift + right-click the air, or right-click a filled cup on a table to clink glasses for 30 s Luck I + 5 s Regeneration I and +1 bonus hunger on the next sip; all 6 glass cup models and textures are redesigned. ([Tableware & Drinks](tableware.html#cheers))<!--清单#118--><!--清单#122-->

### Guide search, mod menu settings, JEI / REI, and Jade overlay integration

- **Full Hotpot Guide parity & in-book search:** the in-game guide now matches all 9 chapters and 56 entries of the web book tutorial (including tables) in 3 languages, with horizontally scrolling top bookmarks and a top-right compass (`Ctrl + F` / `/`) for full-text search. ([What's in the mod?](index.html#guide))<!--清单#113--><!--清单#128-->
- **Mod menu visual settings screen & expanded config:** open "Config" from the mod list to adjust 4 tabs of sliders (including 0%–100% steam/smoke particle intensity, broth speeds, 19 ingredient cook times, and soup/seasoning/drink/food values; requires Mod Menu on Fabric, built-in on NeoForge / Forge). ([FAQ](faq.html#config))<!--清单#110--><!--清单#114--><!--清单#128-->
- **JEI & REI recipe viewer integration:** browse Hotpot Simmering, Cutting Board Slicing, and Rolling Pin Pounding recipe categories and workstation catalysts directly in JEI or REI.<!--清单#115-->
- **Jade & block info overlay integration:** tabletop tableware, drink bottles and cups, stove-mounted pots, cutting-board ingredients, sauce stations, and drink cauldrons show per-part names, clean item icons, and detailed contents (seasoning icons/effects/bites, plate/bowl food counts and dipped sauces/broths, chopstick food, and cooked/raw pot ingredients), updating frame-by-frame across items in single-player and multiplayer. ([Tableware & Drinks](tableware.html#plate))<!--清单#129--><!--清单#130--><!--清单#131-->
- **24 advancements & quality-of-life improvements:** dedicated "Orient's Hotpot Chronicles" advancement tab with 24 advancements (and fixed "Author's Pick" trigger); ray-plane intersection for accurate low-angle pot cell targeting; tighter food placement on 1×1 small dishes; block-atlas stove firebox rendering for HD/animated resource packs; main-menu splash text; pickaxe/axe mining tags; and Traditional Chinese `zh_tw` terminology refinements.<!--清单#103--><!--清单#104--><!--清单#107--><!--清单#109--><!--清单#111--><!--清单#112--><!--清单#121--><!--清单#124--><!--清单#126-->
- **Fixed ladders / vines not climbable and axes mining wood slower**: removed leftover data from an unused experiment, so vanilla climbing (ladders, vines, scaffolding) and axe mining speed work normally again, and no recipe errors appear when entering a world.<!--清单#132-->
<!-- mryt233 改，2026-11-02：0.2.1-alpha 只放宽 NeoForge 最低版本。 -->
## 0.2.1-alpha{{since:0.2.1-alpha}} {#v021}

Released on 2026-11-02. **Gameplay and content are exactly the same as 0.2.0-alpha.** The only change is that the 1.21.1 NeoForge build now needs NeoForge **21.1.186** or later instead of 21.1.265, so older NeoForge versions work too. Fabric requirements are unchanged (Fabric Loader 0.15.11+ and Fabric API). The files are `orients_hotpot_chronicles-1.21.1-fabric-0.2.1-alpha.jar` and `orients_hotpot_chronicles-1.21.1-neoforge-0.2.1-alpha.jar`. See [Downloads, Installation & Feedback](install.html#versions).<!--清单#102-->

## 0.2.0-alpha{{since:0.2.0-alpha}} {#v020}

Released on 2026-11-02. **Available only for Minecraft 1.21.1 Fabric and NeoForge.** Both jars bundle the required framework; just put one in your mods folder. 1.20.1 Fabric / Forge stays on 0.1.4-alpha and 1.20.4 Fabric on 0.1.1-alpha; these older ports are no longer maintained. No item or block IDs were removed or renamed compared with 0.1.4-alpha, but backing up your world before upgrading is still recommended.

### New content

{{icons:minced_pork,raw_ham_sausage,ham_sausage,ham_sausage_slice,cooked_ham_sausage_slice}}

- **Minced Pork and ham sausages:** cut a raw porkchop into Minced Pork; craft Minced Pork + wheat into 3 Raw Ham Sausages; cook them into Ham Sausages; cut those into 3 Ham Sausage Slices; heat the slices again for Cooked Ham Sausage Slices. ([Ingredients & Food Preparation](ingredients.html#sausage))<!--清单#72--><!--清单#75--><!--清单#76--><!--清单#77-->
- **Eat even when full:** every food in this mod can be eaten with a full hunger bar; the hunger bar does not rise, while saturation is still added up to 20. Compatible with Always Eat. ([Plates, Bowls & Drinks](tableware.html#chopsticks))<!--清单#74--><!--清单#99-->
- **Hoppers and pots:** a hopper above the pot only inserts cookable raw ingredients; a hopper below only pulls out cooked food. ([Stoves & Hotpots](hotpot.html#hopper))<!--清单#95-->
- **Numeric configuration:** `config/orients_hotpot_chronicles.json` adjusts food and drink nutrition, broth speed, fuel, dipping sauces, and more. An administrator running `/orientshotpot reload` applies it immediately, and multiplayer servers sync it automatically. ([FAQ](faq.html#config))<!--清单#78--><!--清单#81--><!--清单#96-->
- **NeoForge matches Fabric:** both 1.21.1 loaders now share one codebase and the same content.<!--清单#83-->

{{recipe:raw_ham_sausage}}

### Changes

- **Putting food back:** **sneak + right-click** a bowl or plate while holding food to put it back (the chopsticks stay in hand); only empty chopsticks rest on the rim, so the two no longer conflict.<!--清单#84-->
- **Food in the pot:** when several portions take turns at the surface, they keep the same size; the current one sinks and the next floats up, with no sudden swap when lighting or putting out the fire.<!--清单#92--><!--清单#98-->
- **Campfire pots:** the screen shows the pot heating instead of “no fire”.<!--清单#93-->
- **Tall stove pots:** mining the pot from above a tall stove removes only the pot.<!--清单#94-->
- **Cutting board:** still works with built-in recipes if a modpack removes the stonecutter recipes.<!--清单#73-->
- **Recipe viewers:** tags such as “cuttable ingredients” and “heat sources” show readable names.<!--清单#82-->
- **Config warnings** follow each player’s language; on Fabric servers, a client/server version mismatch is reported when joining.<!--清单#100-->
- **Translation review:** sausage names in several languages are more accurate, and Traditional Chinese wording now matches official terms.<!--清单#101-->

### Fixes

- **Server freezes:** servers with tall stoves or sauce stations no longer freeze when players join or teleport.<!--清单#89-->
- **Long-distance teleport crash:** after respawning or teleporting far away, the client no longer crashes on stoves left in old chunks.<!--清单#88-->
- **Stove fuel:** fuel added while no pot is on the stove survives restarts; stove and sauce station screens close when you walk away or the block is broken.<!--清单#90-->
- **Cooking time after restart:** uncooked food in the pot continues with the correct remaining time.<!--清单#91-->
- **Pot bubbles:** no more fast-then-slow jump while heating up, and smooth at high frame rates.<!--清单#87-->
- **Compatibility:** broths and fuels from other mods are recognized by cooking; a failing compatibility module only skips itself.<!--清单#80--><!--清单#85-->
- **NeoForge chopsticks** no longer error when picking up food; chopsticks holding food in old worlds still load.<!--清单#86-->
- Internal cleanup: add-on extension points and lighter pot calculation and syncing; gameplay unchanged.<!--清单#79--><!--清单#97-->

## 0.1.4-alpha{{since:0.1.4-alpha}} {#v014}

Released on 2026-10-29. **0.1.4-alpha currently covers 1.21.1 Fabric only**; 1.21.1 NeoForge and 1.20.1 Fabric / Forge remain on 0.1.3-alpha-hotfix1.

:::warn
**Renamed Yuanyang pots:** S-shaped is now **Taiji Yuanyang**, and O-shaped is now **Round-Center Yuanyang**, including their item IDs. **The four renamed pots already placed in old worlds will disappear** (two each in copper and iron). Back up your world before upgrading.
:::

## Shrimp, peeled shrimp, and shrimp paste {#shrimp}

{{icons:shrimp,peeled_shrimp,shrimp_paste}}

- **Fishing** can yield shrimp.
- Use a knife on the cutting board, or a stonecutter, to peel shrimp.
- Pound peeled shrimp 3 times with a rolling pin to make shrimp paste.
- All three cook in a hotpot or by roasting. Cooked portions restore **3 / 3 / 4** hunger points respectively.<!--清单#56-->

{{recipe:peeled_shrimp,cooked_shrimp_paste}}

## Orient Cola and cups {#cola}

{{icons:orient_cola,empty_orient_cola_bottle,glass_cup,orient_cola_cup}}

- **Orient Cola:** 4 sips per bottle, with Speed and Haste for 15 seconds per sip. The square bottle bubbles.<!--清单#35--><!--清单#59--><!--清单#62-->
- **Glass cups:** right-click an empty cup with a drink to pour one sip. Drinking leaves an empty cup; cups can sit on tables.<!--清单#37-->

{{recipe:orient_cola,glass_cup}}

### Water and milk cups

{{icons:water_cup,milk_cup}}

- Fill an empty cup from a water / milk bucket; **the bucket remains full**. An empty cup can also scoop water from a source.
- Water has no effect; milk clears all effects. Neither restores hunger.<!--清单#68-->

### Drinks in cauldrons

{{icons:minecraft:cauldron,orange_drink,orient_cola,plant_protein_drink}}

- Pour bottles or filled cups into a vanilla **cauldron**, holding one drink type and up to 12 sips.
- An empty cup scoops one sip; a glass bottle or that drink's empty bottle scoops a bottle.<!--清单#67-->

## Drinks and fruit {#drinks}

{{icons:orange,orange_drink,plant_protein_drink}}

- **Oranges** drop from oak and dark oak leaves.
- Both older drinks have recipes: 3 oranges (or 3 soybeans) + sugar + glass bottle.<!--清单#34-->

{{recipe:orange_drink,plant_protein_drink}}

## New seasonings and broths {#base}

{{icons:garlic,garlic_paste,cooked_kelp,tomato,tomato_hotpot_base,three_fresh_hotpot_base}}

- **Garlic** drops from grass. Pound it 3 times on a board to make 2 **Garlic Paste**, the tenth seasoning (Resistance for 8 seconds).<!--清单#33-->
- **Cooked Kelp:** cook vanilla kelp in the hotpot; restores 2 hunger points.<!--清单#36-->
- **Tomatoes** drop from grass. Add hotpot base to Clear Broth for Tomato / Three Delicacies Broth. Scoop it into an empty bucket, then pour it back to restore that broth.<!--清单#38-->

{{recipe:tomato_hotpot_base,three_fresh_hotpot_base,tomato_soup_bucket}}

## New Yuanyang pots {#yuanyang}

{{icons:copper_taiji_yuanyang_hotpot,copper_round_yuanyang_hotpot,copper_square_yuanyang_hotpot}}

- Added **Square-Center Yuanyang pots**.<!--清单#39-->
- Renamed S-shaped and O-shaped pots to **Taiji Yuanyang** and **Round-Center Yuanyang**, including item IDs; these pots will disappear from old worlds.<!--清单#66-->

{{recipe:copper_square_yuanyang_hotpot}}

## Stone and metal tables {#stone}

{{icons:smooth_stone_hotpot_table,polished_granite_hotpot_table,polished_deepslate_hotpot_table,quartz_hotpot_table,cut_copper_hotpot_table,iron_hotpot_table}}

9 tabletop materials: smooth stone, four polished stone types, polished blackstone, quartz, cut copper, and iron. Low and tall variants make 18 tables.<!--清单#51-->

{{recipe:smooth_stone_hotpot_table}}

## The in-game Hotpot Guide {#guide}

{{icons:hotpot_guide}}

- Right-click to open a red book with category bookmarks on the left and entry bookmarks above. Opening and closing play book sounds. This website's book style is based on it.<!--清单#41--><!--清单#52--><!--清单#60-->
- An advancement framework is ready, but there are no advancements yet.<!--清单#40-->

{{recipe:hotpot_guide}}

## Gameplay changes {#changes}

- **Pots can float:** standalone pots stay in place with air underneath.<!--清单#65-->
- **Take what you see:** chopsticks, empty hands, and bowls select the food currently visible at the surface; raw food triggers a message.<!--清单#30--><!--清单#43-->
- **Rest chopsticks on bowls and plates:** Shift + right-click to rest them; right-click with an empty hand to retrieve.<!--清单#69-->
- **Seating connects up to 3 blocks**, with continuous wood grain even when placed from opposite sides.<!--清单#49--><!--清单#50-->
- **Campfire pots sit 4 pixels lower**, and square-center pots now have supports too.<!--清单#48-->
- **Copper nine-grid pots** now use the same arrangement as iron: copper ingot, cauldron, copper ingot in a row.<!--清单#57-->
- Creative **middle-click copies the whole pot**, including broth and food.<!--清单#44-->
- Tooltips list sauce effects and add small flavor lines to some food items.<!--清单#32-->
- Creative subcategories are All / Kitchenware / Tableware / Furniture / Food. Kitchenware uses a knife icon, and new items are categorized.<!--清单#31--><!--清单#45--><!--清单#46-->

## Appearance and fixes {#looks}

- Sauce station screens now use wooden frames and light text, matching the stove.<!--清单#53-->
- The firebox screen shows the stove's actual block appearance.<!--清单#64-->
- A thicker knife handle; refreshed garlic, garlic paste, shrimp paste, cups, cooked kelp, hotpot base, and tofu.<!--清单#54--><!--清单#55--><!--清单#58-->
- An angled cutting-board icon and better-centered inventory icons.<!--清单#42--><!--清单#63-->
- Removed an extra bowl floating beside sauce stations.<!--清单#47-->
- Removed a duplicate built-in resource pack, almost halving jar size without changing gameplay.<!--清单#61-->

{{icons:iron_kitchen_knife,garlic_paste,shrimp_paste,cooked_kelp,tofu}}

### Other changes

- **Tall stove recipe:** place a brick block directly above a stove, rather than in any arrangement.<!--清单#70-->
- **Translations in 9 languages** were proofread; gameplay and values are unchanged.<!--清单#71-->

## 0.1.3-alpha-hotfix1{{since:0.1.3-alpha-hotfix1}} {#v013}

Released on 2026-10-25. **0.1.3-alpha was not released separately; its contents have all been merged into this build**, so below is the full list of changes from 0.1.2-alpha to this version.

**Compatibility with older world saves has been removed. Renamed items in worlds from 0.1.2-alpha or earlier (such as placed drinks and Rich Stock pots) may disappear. Please play in a new world.**

### New content

- **Beef Loin and Beef Loin Slices:** cows and mooshrooms have a 25% chance to drop loin. Cooked loin restores 8 hunger points. Slice it into 3 slices on a cutting board or in a stonecutter; these are the fastest-cooking hotpot slices. ([Ingredients & Food Preparation](ingredients.html#loin))<!--清单#13-->
- **Yuanyang and four-grid pots:** S-shaped, O-shaped, and straight-divider Yuanyang pots, plus a four-grid pot, each with one broth per compartment, in copper and iron. Crafted with one ingot + a nine-grid pot. Each compartment has its own broth; aim at a compartment to add or scoop broth, and each slot cooks in its own broth. ([Stoves & Hotpots](hotpot.html#yuanyang))<!--清单#20--><!--清单#21-->

### Changes

- **New chopsticks gameplay:** hold bamboo chopsticks and **Shift + right-click the pot** to open the hotpot screen; **right-clicking the pot directly** picks up a cooked portion from the targeted cell, which can then be eaten by holding right-click. Right-clicking a pot, bowl, or plate while holding cooked food eats it, returning it only if too full to eat; right-clicking a pot while holding raw food adds it to the pot as before.<!--清单#29-->
- **Dipping sauces are easier to use:** holding dipping sauce in your **main hand or off hand** automatically dips each bite when eating (previously only the off hand was checked); bamboo chopsticks holding food can also **right-click a sauce bowl placed on a table** to take a dip, turning the sauce bowl back into an empty bowl after the last dip.<!--清单#29-->
- **New crafting recipes for cutting boards and small dishes**, no longer conflicting with vanilla recipes: cutting boards are now crafted from **two logs of any type placed side by side horizontally**, producing 2 boards; all 5 small dish types (bamboo, copper, iron, quartz, terracotta) are now crafted from **“material bowl material” placed horizontally**, still producing 2 dishes.<!--清单#28-->
- **Proofread names and tooltips in 9 languages** (English, Traditional Chinese, Japanese, Korean, Russian, German, French, Spanish, Portuguese): several names are more accurate, and Traditional Chinese wood names now use official Taiwan translations; gameplay and stats remain unchanged.<!--清单#27-->
- **Creamy Broth renamed to Rich Stock** (the broth made with a milk bucket), with translated names updated accordingly.<!--清单#23-->
- **Packed Yuanyang / four-grid pots:** item icons show each compartment's broth, and tooltips list broths by compartment.<!--清单#22-->
- **Pots accept only food:** wood and iron items previously cooked into charcoal and iron nuggets; now only food can be added.<!--清单#7-->
- **Sauce stations lowered by one trapdoor's height**, making all three tiers easier to reach.<!--清单#1-->
- **Cutting boards have a facing direction**, with food facing the player placing it.<!--清单#5-->
- **Mining food on a board removes only the food:** the board remains; food and board directions are stored separately.<!--清单#11-->
- **Cutting boards are visible in hand**, and inventory icons are no longer dark.<!--清单#9--><!--清单#19-->
- **Rolling Pins** have a new design with handles.<!--清单#10-->
- **Campfire pots:** iron supports at the four bottom corners rest on the campfire logs, so the pot no longer appears to float and flames no longer pass through it.<!--清单#2-->
- **Food in cold pots** no longer flickers; it slowly bobs up and down on a roughly 6-second cycle.<!--清单#16-->
- **Benches connect even when placed from the opposite side:** benches along the same line connect regardless of which side you stand on when placing them.<!--清单#17-->
- **Sauce station selection outlines** surround only the bowl under the crosshair.<!--清单#8-->
- **Standalone / campfire pots:** can be packed into your inventory whole with an empty hand and Shift + right-click.<!--清单#15-->

### Fixes

- **Food no longer jitters back and forth when eaten by holding right-click with chopsticks from bowls or plates**, eliminating the issue where food seemed unable to be picked up.<!--清单#29-->
- Organized recipe-book groups for stools, benches, and plates to match current item names; removed unused old files.<!--清单#24-->
- Aligned internal drink names with item names (no visible change, but **drinks placed in old test worlds may disappear**).<!--清单#25-->
- Aligned internal names for Rich Stock, seating, plate models, and other assets; removed compatibility for old world saves (**renamed items in old test worlds may disappear**).<!--清单#26-->
- The cutting board can process **potatoes and tofu** again (they could not be placed on the cutting board in 0.1.2-alpha).<!--清单#18-->
- Food no longer leaves a ghost image after a cutting board is emptied.<!--清单#6-->
- Eating with chopsticks creates food-colored crumbs instead of purple-and-black missing-texture particles.<!--清单#4-->
- Breaking a Plant Protein Drink creates particles in its own colors rather than those of Orange Drink.<!--清单#14-->
- Added the square “回” motif to the upper half of the Orange Drink bottle.<!--清单#3-->
- The top of the Plant Protein Drink bottle cap no longer flickers.<!--清单#12-->

## 0.1.2-alpha{{since:0.1.2-alpha}} {#v012}

Released on 2026-10-23. GitHub release pages group Fabric and Forge / NeoForge builds together.

### New content

- **Sauce stations** in 11 woods and iron: three tiers with nine bowls, 1.5 blocks tall. Right-click to restock; each bowl's seasoning depends on its ingredient.
- **Nine seasonings:** MSG, chili oil, sweet-and-sour sauce, honey, sesame oil, vinegar, sugar, chopped scallions, and chopped cilantro. Their effects stack. Use a bowl on the station to mix sauce; one bowl lasts nine dips.
- **Automatic dipping:** hold sauce in the off hand when eating in the hotpot screen or with chopsticks to dip automatically.
- **Scallions, cilantro, and soybeans** drop from grass.
- **Cutting Board, Iron Kitchen Knife, and Rolling Pin.**
- **Cutting:** raw beef / mutton become fatty beef / mutton rolls; potatoes become slices; tofu becomes cubes. A stonecutter works too.
- **Pounding meatballs:** use a rolling pin three times on a cutting board to make beef, pork, mutton, or fish balls, with cilantro variants too. Meatballs are small 3D spheres.
- **Craft tofu with soybeans and a water bucket.**

### Changes

- Smaller stove openings, short-stove brick textures aligned with vanilla bricks, and front-facing stove inventory icons.
- Clear Broth now has a clear, light color.
- Tall hotpot tables, wooden sauce stations, and cutting boards are now flammable.

### Fixes

- Stoves, plates, and seats missing from 0.1.0-alpha.1 worlds can now be recognized again; old stools become benches.
- Fixed middle-click copying of drinks and food on hotpot tables, and pots on stoves.
- Selection outlines now surround only the targeted object when several share a block.
- Item names and properties now appear when hovering in the hotpot and firebox screens.

## 0.1.1-alpha{{since:0.1.1-alpha}} {#v011}

Released on 2026-10-23.

### New content

- **New bowl uses:** vanilla bowls can hold food on tables, collect cooked food from pots, or serve soup. Clear Broth, Rich Stock, Mushroom Broth, and Spicy Red Broth have different effects.
- **Drinks:** Orange Drink and Plant Protein Drink. Each bottle has four sips and can be placed on tables.
- **Campfire hotpot:** pots cook on lit campfires and soul campfires at the same speed as stoves, without consuming fuel, and heal nearby players.
- **Small decorations** such as lanterns, flower pots, candles, and cakes sit flush with hotpot tabletops.

### Fixes

- Fixed jittering when standing on plates on low tables, and stuttering ingredient bobbing in old worlds.

### Notes

- Several item / block IDs were aligned with English names, including `brick_stove`, `oak_stool`, and `oak_bench`. **Items from this mod may disappear or change in worlds from 0.1.0-alpha.1. Back up first; a new world is recommended.**

## 0.1.0-alpha.1{{since:0.1.0-alpha.1}} {#v010}

Released on 2026-10-22. First public version, including:

- **Short and tall stoves, copper and iron pots**, four broths, fuel and heat rules, hotpot and firebox screens, and eating cooked food with chopsticks.
- **Healing around the pot:** heated pots refresh Regeneration I within six blocks; eating from cold pots may cause Nausea.
- **Pack the whole pot:** broth and food stay inside the pot item when picked up.
- **Low and tall hotpot tables, stools, and benches in 11 woods**, with automatic connections and seating.
- **Plates:** five materials × six sizes; multiple plates can share one block on a table.
- **Tripe:** drops from cows and mooshrooms and cooks faster than beef.
- Creative inventory categories and smaller category tabs on the left.
- Ten languages.
