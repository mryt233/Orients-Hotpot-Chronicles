<!-- 作者：mryt233；英文翻译，原文：mryt233。发布前由 mryt233 同步并审核。 -->
<!-- mryt233 改，2026-10-30：同步正式版 #30–#71 和未发布 #72–#82；保留原译者署名。 -->
# Frequently Asked Questions

## Pots and stoves

**Q: There is broth in the pot. Why won't the stove light?**
Check three things: ① fuel in the firebox (right-click the brick body to look); ② a pot actually sitting on the stove; ③ broth actually poured inside (an empty pot will not light the stove). Once lit, the pot gives off steam and the broth boils.

**Q: The stove is lit, but the fuel is not decreasing.**
That's normal. Fuel is used only **while raw ingredients need cooking**. Heating broth without raw food is “idle simmer” and does not consume fuel (see [Stoves & Hotpots](hotpot.html#fuel)).

**Q: I can't put something into the pot.**
Put **food** in the pot; cooked food cannot go back in (from 0.1.3-alpha-hotfix1 onward, wood, iron items, and ores are rejected entirely). The targeted large cell may also be full (nine small slots); try another cell.

**Q: How do I change the broth?**
**Right-click the pot with an empty bucket** to scoop the broth back into the bucket (take out food first if any remains). Mushroom broth requires a bowl, and also requires that no food is left in the pot.

**Q: Why isn't the food cooking?**
- Is the pot a “cold pot” (not on a lit stove or campfire)? Cold pots do not cook food.
- Check heat: wood ×0.65, coal ×1.0, lava bucket ×1.6; broths: Rich Stock is the slowest, Spicy Red Broth is the fastest. Wood heat + Rich Stock will be very slow.
- Ingredients have different cooking times: whole beef takes 5 seconds, fatty beef rolls take 2.5 seconds (beef loin slices take a little over 1 second; see the cooking table in the [Recipe Collection](recipes.html)).

**Q: I suddenly got Nausea while eating.**
You ate from a **cold pot**, which has a 30% chance of causing Nausea for 15 seconds. Drinking a **Plant Protein Drink**, a bowl of **Clear Broth**, or dipping sauce with **vinegar** clears it.

**Q: The tall stove says there is not enough room.**
A tall stove occupies the block above it too. Clear the space above it.

**Q: How do I pick up a pot?**
For a stove-mounted pot: **Shift + right-click with an empty hand**. The whole pot goes into your inventory with broth and food, and is restored when placed (standalone / campfire pots gain this action from 0.1.3-alpha-hotfix1 onward). You can also mine it directly (the pot has its own hardness, separate from the stove).

## Tables, seating, and tableware

**Q: I can't sit down.**
Right-click with an empty hand (or while holding something that is not a block). Leave at least one full block between seated players; try moving one block along if someone is already beside you.

**Q: I can't place another plate in this block.**
Plates cannot overlap. One block fits up to 5 small dishes, 2 long plates, or 1 square plate. When mixing types, it depends on the occupied space.

**Q: I can't select something on a tall table.**
The mod accounts for this: plates on the tabletop can be selected under your crosshair. If you still cannot select one, aim slightly to the side or try sneaking.

**Q: How do I pick up the whole plate?**
Right-click the plate with an empty hand to take the whole plate with its food. Mining also works, breaking only the targeted plate.

## Ingredients

**Q: How do I get tripe?**
Kill cows or mooshrooms.

**Q: How do I get soybeans, scallions, and cilantro?**
**Break short or tall grass** (do not use shears). Scallions and cilantro each have a 25% drop chance; soybeans have 15%.

**Q: I can't put something on the cutting board.**
Only ingredients that **can be cut or pounded** work: raw beef, raw mutton, beef loin, potatoes, tofu (the last two could not be placed in 0.1.2-alpha, requiring a stonecutter), and raw porkchops, cod, and salmon for pounding meatballs. Cooked food cannot be placed.

**Q: Why do drinks have no crafting recipes?**
Recipes were added in 0.1.4-alpha. Orange Drink uses oranges, Plant Protein Drink uses soybeans, and Orient Cola uses cocoa beans and honey. See [Tableware & Drinks](tableware.html#drink).

## Worlds and versions

**Q: Stoves, plates, or seats disappeared from my old world.**
Upgrading a 0.1.0-alpha.1 world can lose or change some items (there was a major item ID rename). From 0.1.1-alpha onward, most old IDs can be recognized again, and old stools become benches. **Always back up worlds before upgrading.** A fresh world is recommended during alpha.

**Q: Can this mod integrate with other mods?**
An integration hook exists: blocks in the `orients_hotpot_chronicles:heat_sources` block tag can act as heat sources for pots. Suggestions for other integrations are welcome on GitHub.

**Q: How do 1.20.1, 26.1.x, and 26.2 differ from 1.21.1?**
In 0.2.2-alpha, **1.21.1 (Fabric / NeoForge), 1.20.1 (Fabric / Forge), 26.1.x (Fabric / NeoForge), and 26.2 (Fabric / NeoForge)** are all updated simultaneously with the same gameplay and content. See [Downloads, Installation & Feedback](install.html#ports).

**Q: Where should I report a bug?**
Visit the GitHub repository: <https://github.com/mryt233/Orients-Hotpot-Chronicles> and describe it clearly in Issues (or join **QQ group 248314791**; contact info is in [Downloads, Installation & Feedback](install.html#feedback)). Please specify: Minecraft version, mod version, how to reproduce it, and preferably screenshots.

<!-- mryt233 改，2026-11-02：0.2.0-alpha 发布，数值配置转正式。 -->
## Configurable values {#config}{{since:0.2.0-alpha}}

**Q: Can I change how much hunger food restores, or how fast broths cook?**
Yes. Starting in 0.2.2-alpha, you can select this mod in the **Mods menu** (install Mod Menu on Fabric; built-in on NeoForge / Forge) and click **Config** to adjust the values and save immediately. You can also edit `config/orients_hotpot_chronicles.json` directly (adjusting food nutrition, per-sip drink nutrition, broth speed, fuel, dipping sauces, and more, with each entry's meaning, range, and unit documented). After editing the JSON file, an administrator runs **`/orientshotpot reload`** to apply changes without restarting; the reply reports how many warnings there were.

- On **multiplayer servers**, edit only the server file; players receive it when joining and after each reload.
- A bad entry (not a number, out of range, or a misspelled key) falls back to its default with a warning; if the whole file is broken, all defaults are used and **the broken file is kept as-is** so you can fix it.
- Change recipes with a data pack, then run vanilla `/reload`.
- Numbers in this guide are defaults; if a server changed its config, the server's values apply.

### Using the settings screen {#screen}

{{since:0.2.2-alpha}}The screen has two tabs, **Common** and **Advanced**, and opens on **Common**:

- **Common** holds only the few things players change most — the **particle intensity** slider at the top (plus 0% / 50% / 100% shortcuts), then **coal-grade stove heat** (the master knob for how fast everything cooks), **fallback cook time**, and the **heat multiplier of spicy / clear / rich-stock broth**.
- **Advanced** moves everything else into collapsible groups by category: cooking times, per-ingredient cooking times, broth heat speed, fuel grades, soup bowls, seasonings, drinks, and food nutrition. Groups start collapsed; click a group title to expand it.
- **Changing values**: each row is `- value unit +`. One click steps once; **hold Shift to step by 10**. **Click the value box to type a number directly**; typing something that is not a number or out of range turns it **red and blocks saving**.
- **Units**: time is shown in ticks (20 ticks = 1 second, with the seconds also shown when there is room), multipliers as ×, hunger and XP as points, effect levels as levels.
- **Per-row reset**: a **↺** appears on rows you changed, and resets only that entry. Unchanged rows do not show it.
- **Reset everything**: the bottom Reset button turns into **click again to confirm**; a second click within 4 seconds really resets everything (and sets particle intensity back to 100%).
- **Hints**: hover any row to see a plain-language explanation plus “range x–y, default z”.
- Scroll with the **mouse wheel** or the scrollbar when the list is long. **Save & Apply** writes the config file and hot-reloads; ESC or Cancel discards every change (particle intensity goes back to its pre-screen value).

## Useful commands

```
/give @s orients_hotpot_chronicles:brick_stove
/give @s orients_hotpot_chronicles:iron_nine_grid_hotpot
/give @s orients_hotpot_chronicles:orange_drink
```

All item IDs are listed in the [Item Catalog](items.html), formatted as `orients_hotpot_chronicles:<ID>`.
