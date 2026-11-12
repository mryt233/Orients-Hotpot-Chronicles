package cn.blockforge.generated.orientshotpotchronicles.hotpot;

/**
 * 锅的汤底状态（从 {@link HotPotBlockEntity} 分出）。
 * <p>
 * 只管「每边倒的是哪种汤、这格泡在哪种汤里、方块状态该显示什么汤面」，
 * 不碰库存、熟度和经验。状态存在主类的 {@code bases} 数组里（同包可见）。
 * 作者：mryt233 */
final class HotPotSoup {
    private HotPotSoup() {
    }

    static int base(HotPotBlockEntity be) {
        return SoupIds.index(be.bases[0]);
    }

    static SoupVisual visual(HotPotBlockEntity be) {
        return SoupVisual.fromDef(Soups.get(be.bases[0]));
    }

    /** 锅里有没有汤（鸳鸯 / 田字锅任一边有就算）。 */
    static boolean has(HotPotBlockEntity be) {
        for (String id : be.bases) {
            if (!id.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** 某一边的汤底编号（-1 = 没汤）。 */
    static int baseOfSide(HotPotBlockEntity be, int side) {
        return side >= 0 && side < be.bases.length ? SoupIds.index(be.bases[side]) : SoupIds.EMPTY;
    }

    static void setBaseOfSide(HotPotBlockEntity be, int side, int id) {
        if (side >= 0 && side < be.bases.length) {
            if (id == SoupIds.UNKNOWN) throw new IllegalArgumentException("未知汤底不能从索引恢复 ID");
            be.bases[side] = SoupIds.id(id);
        }
    }

    /** 这一格泡在哪种汤里（不用的格子 / 那边没汤 = null）。 */
    static SoupDef forSlot(HotPotBlockEntity be, int slot) {
        int side = be.layout().sideOfSlot(slot);
        return side == PotLayout.NONE ? null : Soups.byIndex(baseOfSide(be, side));
    }

    /** 拿碗盛汤盛哪边：准星那格那边有汤就盛那边，没有就盛另一边。 */
    static int bowlSoup(HotPotBlockEntity be, int bigCell) {
        int side = be.layout().sideOfCell(bigCell);
        int id = side == PotLayout.NONE ? -1 : baseOfSide(be, side);
        if (id != SoupIds.EMPTY) {
            return id;
        }
        for (String other : be.bases) {
            int index = SoupIds.index(other);
            if (index != SoupIds.EMPTY) {
                return index;
            }
        }
        return -1;
    }

    /** 方块状态里的汤面：九宫格锅照汤底；鸳鸯锅的汤面由渲染器按两边画，方块状态一律空。 */
    static SoupVisual blockVisual(HotPotBlockEntity be) {
        if (be.layout().multiSoup()) return SoupVisual.NONE;
        // 未知 ID 暂时不能解析时，沿用方块已有汤面，不把未知汤修成空锅；若仍为 NONE 则兜底用 OTHER（4F-3）。
        if (SoupIds.index(be.bases[0]) == SoupIds.UNKNOWN) {
            var state = be.getBlockState();
            SoupVisual existing = SoupVisual.NONE;
            if (StoveBlock.isStove(state)) existing = state.getValue(StoveBlock.SOUP);
            else if (state.getBlock() instanceof HotPotBlock) existing = state.getValue(HotPotBlock.SOUP);
            return existing != SoupVisual.NONE ? existing : SoupVisual.OTHER;
        }
        return SoupVisual.fromDef(Soups.get(be.bases[0]));
    }
}
