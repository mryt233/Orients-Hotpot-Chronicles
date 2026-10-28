package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.util.StringIdentifiable;

/** 方块状态使用的汤底外观；NONE 代表空锅。 */
public enum SoupVisual implements StringIdentifiable {
    NONE("none"),
    CLEAR("clear"),
    RICH_STOCK("rich_stock"),
    MUSHROOM("mushroom"),
    SPICY("spicy"),
    TOMATO("tomato"),
    THREE_FRESH("three_fresh");

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
        if (base == null) {
            return NONE;
        }
        // 以后新加的汤底如果还没有九宫格锅的方块状态汤面，这里不报错，按空锅显示（鸳鸯 / 田字锅由渲染器按贴图画，不受影响）
        for (SoupVisual v : values()) {
            if (v.name().equals(base.name())) {
                return v;
            }
        }
        return NONE;
    }
}
