package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.util.StringRepresentable;

/**
 * 方块状态使用的汤底外观；NONE 代表空锅。
 * <p>mryt233 改（2026-11-10，r147）：和 {@link SoupBase} 的对应关系改成「按名字自动推」——
 * 每个有汤的 {@code SoupBase} 都必须有一个同名的 {@code SoupVisual}（方块状态里绘汤面要用），
 * 启动时由 {@link #verifyAgainstSoupBases()} 核对：漏一个直接抛出带说明的异常，
 * 不再像以前那样悄悄按空锅显示。空锅由 {@link #NONE} 表示，不参与核对。</p>
 * 作者：mryt233 */
public enum SoupVisual implements StringRepresentable {
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
    public String getSerializedName() {
        return name;
    }

    public boolean hasSoup() {
        return this != NONE;
    }

    public static SoupVisual fromBase(SoupBase base) {
        if (base == null) {
            return NONE;
        }
        return base.visual();
    }

    /**
     * mryt233 改（2026-11-10）：启动时核对每种汤底都有对应的汤面方块状态值。
     * 少一个说明「加了汤底但没在 SoupVisual 里补一个值」，方块状态里画不出汤面，必须当场报错。
     */
    public static void verifyAgainstSoupBases() {
        for (SoupBase base : SoupBase.values()) {
            boolean found = false;
            for (SoupVisual visual : values()) {
                if (visual != NONE && visual.name.equals(base.textureKey())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException(
                    "汤底 " + base.name() + "（textureKey=" + base.textureKey() + "）在 SoupVisual 里没有对应的汤面值；"
                        + "加汤底时必须在 SoupVisual 补一个同名值（顺序加在最后），否则方块状态画不出汤面。");
            }
        }
    }
}
