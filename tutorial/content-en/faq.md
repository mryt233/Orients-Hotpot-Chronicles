<!-- 作者：；英文翻译。 -->
# Frequently Asked Questions

## Pots and stoves

**Q: There is broth in the pot. Why won't the stove light?**
Check three things: fuel in the firebox (right-click the brick body to look), a pot actually attached to the stove, and broth actually inside the pot. A pot without broth will not light the stove. A heated pot gives off steam and has boiling broth.

**Q: The stove is lit, but the fuel is not decreasing.**
That's normal. Fuel is used only **while raw food needs cooking**. Heating broth without raw food does not use fuel; see [Stoves & Hotpots](hotpot.html#fuel).

**Q: I can't put something into the pot.**
Put **food** in the pot; cooked food cannot go back in. From 0.1.3, wood, iron items, and ores are rejected entirely. The targeted large cell may also be full (nine small slots); try another cell.

**Q: How do I change the broth?**
**Right-click with an empty bucket** to scoop the broth back out. Remove food first if any remains. Mushroom broth requires a bowl and an empty pot with no food.

**Q: Why isn't the food cooking?**

- Is the pot cold, without a burning stove or lit campfire? Cold pots do not cook.
- Check heat: wood ×0.65, coal ×1.0, lava bucket ×1.6. Rich Stock is the slowest broth and Spicy Red Broth is the fastest. Wood + Rich Stock is slow.
- Ingredients have different cooking times: whole beef takes five seconds and fatty beef rolls take 2.5 seconds. Loin slices in 0.1.3 take a little over one second; see the cooking table in the [Recipe Collection](recipes.html).

**Q: I suddenly got Nausea while eating.**
You ate from a **cold pot**, which has a 30% chance of causing Nausea for 15 seconds. A **Plant Protein Drink**, **Clear Broth Bowl**, or sauce containing **vinegar** clears it.

**Q: The tall stove says there is not enough room.**
It occupies the block above it too. Clear that space.

**Q: How do I pick up a pot?**
For a stove-mounted pot, **Shift + right-click with an empty hand**. The whole pot goes into your inventory with broth and food and is restored when placed. Standalone / campfire pots gain this action in 0.1.3. You can also mine it directly; the pot has its own mining hardness, separate from the stove.

## Tables, seating, and tableware

**Q: I can't sit down.**
Right-click with an empty hand or a non-block item. Leave a full block between seated players; try moving one block along if someone is already beside you.

**Q: I can't place another plate in this block.**
Plates cannot overlap. One block fits up to five small dishes, two long plates, or one square plate. When mixing types, the occupied space matters.

**Q: I can't select something on a tall table.**
The mod accounts for this height, so plates should be selectable on the tabletop. If you still cannot select one, aim slightly to the side or try sneaking.

**Q: How do I pick up the whole plate?**
Right-click it with an empty hand to take the plate and its food. Mining also works and affects only the targeted plate.

## Ingredients

**Q: How do I get tripe?**
Kill cows or mooshrooms.

**Q: How do I get soybeans, scallions, and cilantro?**
**Break short or tall grass without shears.** Scallions and cilantro each have a 25% drop chance; soybeans have 15%.

**Q: I can't put something on the cutting board.**
Only ingredients that **can be cut or pounded** work: raw beef, mutton, Beef Loin (0.1.3), potatoes, tofu (the last two require a stonecutter in 0.1.2-alpha), and raw porkchops, cod, and salmon for meatballs. Cooked food does not work.

**Q: Why do drinks have no crafting recipes?**
For now, get them from the Creative inventory or with `/give`. Recipes may be added later.

## Worlds and versions

**Q: Stoves, plates, or seats disappeared from my old world.**
Upgrading a 0.1.0-alpha.1 world can remove or change some items because item IDs were renamed. From 0.1.1-alpha, most old IDs can be recognized again, and old stools become benches. **Always back up worlds before upgrading.** A fresh world is recommended during alpha.

**Q: Can this mod integrate with other mods?**
An integration hook exists: blocks in the `orients_hotpot_chronicles:heat_sources` block tag can heat pots. Suggest other integrations on GitHub.

**Q: How do 1.20.1 and NeoForge differ from 1.21.1 Fabric?**
Gameplay is the same, with small differences such as steam particles and returning bowls. **New content usually reaches Fabric 1.21.1 first**, with other builds following later. See [Downloads, Installation & Feedback](install.html#ports).

**Q: Where should I report a bug?**
Visit <https://github.com/mryt233/Orients-Hotpot-Chronicles> and open an Issue, or join **QQ group 248314791**. See [Downloads, Installation & Feedback](install.html#feedback) for contacts. Include your Minecraft and mod versions, reproduction steps, and ideally a screenshot.

## Useful commands

```
/give @s orients_hotpot_chronicles:brick_stove
/give @s orients_hotpot_chronicles:iron_nine_grid_hotpot
/give @s orients_hotpot_chronicles:orange_drink
```

All item IDs are listed in the [Item Catalog](items.html). Use the format `orients_hotpot_chronicles:<ID>`.
