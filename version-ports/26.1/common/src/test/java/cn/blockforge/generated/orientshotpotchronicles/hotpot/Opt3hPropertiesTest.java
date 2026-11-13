// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 无世界的属性包契约：槽位熟度、15 位熟食字及燃料缺省值逐项固定。 */
class Opt3hPropertiesTest {
    @BeforeAll
    static void bootstrap() { Opt3AttributesTest.bootstrapMinecraft(); }

    @Test
    void propertiesZeroThroughNinetyKeepTheirOriginalIndices() throws ReflectiveOperationException {
        HotPotBlockEntity pot = new HotPotBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        pot.bases[0] = SoupIds.legacy(4);
        var progressField = HotPotSlots.class.getDeclaredField("doneness");
        progressField.setAccessible(true);
        int[] progress = (int[]) progressField.get(pot.slots);
        var cookedField = HotPotSlots.class.getDeclaredField("cooked");
        cookedField.setAccessible(true);
        boolean[] cooked = (boolean[]) cookedField.get(pot.slots);
        for (int i = 0; i < 81; i++) progress[i] = 7 + 11 * i;
        for (int i : new int[]{0, 14, 15, 29, 30, 44, 45, 59, 60, 74, 75, 80}) cooked[i] = true;
        int[] expected = {
            4, -1, 0, 0,
            7, 18, 29, 40, 51, 62, 73, 84, 95,
            106, 117, 128, 139, 150, 161, 172, 183, 194,
            205, 216, 227, 238, 249, 260, 271, 282, 293,
            304, 315, 326, 337, 348, 359, 370, 381, 392,
            403, 414, 425, 436, 447, 458, 469, 480, 491,
            502, 513, 524, 535, 546, 557, 568, 579, 590,
            601, 612, 623, 634, 645, 656, 667, 678, 689,
            700, 711, 722, 733, 744, 755, 766, 777, 788,
            799, 810, 821, 832, 843, 854, 865, 876, 887,
            16385, 16385, 16385, 16385, 16385, 33
        };
        assertNull(pot.getLevel());
        assertEquals(91, expected.length);
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], pot.propertyAt(i), "property " + i);
        pot.bases[1] = SoupIds.legacy(1); pot.bases[2] = SoupIds.legacy(2); pot.bases[3] = SoupIds.legacy(3);
        assertEquals(94, HotPotBlockEntity.PROP_COUNT);
        assertEquals(91, HotPotBlockEntity.PROP_EXTRA_BASES);
        assertEquals(1, pot.propertyAt(91));
        assertEquals(2, pot.propertyAt(92));
        assertEquals(3, pot.propertyAt(93));
    }
}
