package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.util.StringIdentifiable;

/** 灶台方块里承载的锅状态；锅与灶台始终是同一个方块。 */
public enum StovePot implements StringIdentifiable {
    NONE("none", null, false),
    COPPER("copper", HotPotMaterial.COPPER, false),
    COPPER_SOUP("copper_soup", HotPotMaterial.COPPER, true),
    IRON("iron", HotPotMaterial.IRON, false),
    IRON_SOUP("iron_soup", HotPotMaterial.IRON, true),
    // 鸳鸯锅坐灶（新增）：名字 = 模型名里的锅名（HotPotMaterial.modelKey）
    COPPER_YUANYANG_S("copper_yuanyang_s", HotPotMaterial.COPPER_S, false),
    COPPER_YUANYANG_S_SOUP("copper_yuanyang_s_soup", HotPotMaterial.COPPER_S, true),
    COPPER_YUANYANG_O("copper_yuanyang_o", HotPotMaterial.COPPER_O, false),
    COPPER_YUANYANG_O_SOUP("copper_yuanyang_o_soup", HotPotMaterial.COPPER_O, true),
    IRON_YUANYANG_S("iron_yuanyang_s", HotPotMaterial.IRON_S, false),
    IRON_YUANYANG_S_SOUP("iron_yuanyang_s_soup", HotPotMaterial.IRON_S, true),
    IRON_YUANYANG_O("iron_yuanyang_o", HotPotMaterial.IRON_O, false),
    IRON_YUANYANG_O_SOUP("iron_yuanyang_o_soup", HotPotMaterial.IRON_O, true),
    COPPER_YUANYANG_SPLIT("copper_yuanyang_split", HotPotMaterial.COPPER_SPLIT, false),
    COPPER_YUANYANG_SPLIT_SOUP("copper_yuanyang_split_soup", HotPotMaterial.COPPER_SPLIT, true),
    IRON_YUANYANG_SPLIT("iron_yuanyang_split", HotPotMaterial.IRON_SPLIT, false),
    IRON_YUANYANG_SPLIT_SOUP("iron_yuanyang_split_soup", HotPotMaterial.IRON_SPLIT, true),
    COPPER_FOUR_GRID("copper_four_grid", HotPotMaterial.COPPER_FOUR, false),
    COPPER_FOUR_GRID_SOUP("copper_four_grid_soup", HotPotMaterial.COPPER_FOUR, true),
    IRON_FOUR_GRID("iron_four_grid", HotPotMaterial.IRON_FOUR, false),
    IRON_FOUR_GRID_SOUP("iron_four_grid_soup", HotPotMaterial.IRON_FOUR, true);

    private final String name;
    private final HotPotMaterial material;
    private final boolean soup;

    StovePot(String name, HotPotMaterial material, boolean soup) {
        this.name = name;
        this.material = material;
        this.soup = soup;
    }

    @Override
    public String asString() {
        return name;
    }

    public boolean hasPot() {
        return material != null;
    }

    public boolean hasSoup() {
        return soup;
    }

    public HotPotMaterial material() {
        return material;
    }

    public StovePot withSoup(boolean hasSoup) {
        if (!hasPot()) {
            return NONE;
        }
        for (StovePot pot : values()) {
            if (pot.material == material && pot.soup == hasSoup) {
                return pot;
            }
        }
        return this;
    }

    public static StovePot of(HotPotMaterial material) {
        for (StovePot pot : values()) {
            if (pot.material == material && !pot.soup) {
                return pot;
            }
        }
        return NONE;
    }
}
