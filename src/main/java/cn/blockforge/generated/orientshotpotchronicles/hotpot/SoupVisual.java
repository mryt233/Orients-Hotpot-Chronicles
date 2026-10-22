package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.util.StringIdentifiable;

/** 方块状态使用的汤底外观；NONE 代表空锅。 */
public enum SoupVisual implements StringIdentifiable {
    NONE("none"),
    CLEAR("clear"),
    MILKY("milky"),
    MUSHROOM("mushroom"),
    SPICY("spicy");

    private final String name;

    SoupVisual(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return name;
    }

    public boolean hasSoup() {
        return this != NONE;
    }

    public static SoupVisual fromBase(SoupBase base) {
        return base == null ? NONE : valueOf(base.name());
    }
}
