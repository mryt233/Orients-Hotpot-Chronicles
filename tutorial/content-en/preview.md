<!-- 作者：mryt233；2026-11-09 r146a 的开发中改动记在这里。 -->
# Development Preview

:::dev
Development Preview (not released yet; names and values may change). Everything added after 0.2.2-alpha is listed here.
:::

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
