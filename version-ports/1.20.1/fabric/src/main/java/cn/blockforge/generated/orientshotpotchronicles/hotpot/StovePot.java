package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.util.StringIdentifiable;

/** 灶台方块里承载的锅状态；锅与灶台始终是同一个方块。 */
public enum StovePot implements StringIdentifiable {
    NONE("none", null, false),
    COPPER("copper", HotPotMaterial.COPPER, false),
    COPPER_SOUP("copper_soup", HotPotMaterial.COPPER, true),
    IRON("iron", HotPotMaterial.IRON, false),
    IRON_SOUP("iron_soup", HotPotMaterial.IRON, true);

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
        if (material == HotPotMaterial.COPPER) {
            return hasSoup ? COPPER_SOUP : COPPER;
        }
        return hasSoup ? IRON_SOUP : IRON;
    }

    public static StovePot of(HotPotMaterial material) {
        return material == HotPotMaterial.COPPER ? COPPER : IRON;
    }
}
