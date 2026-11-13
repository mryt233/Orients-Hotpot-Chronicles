// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 1.20.1 砧板切菜与捣制计数分离（{@code Cuts} / {@code Pounds}）及旧档兼容单元测试。作者：mryt233
 */
class CuttingBoardPoundsTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    private static CuttingBoardBlockEntity newBoard() {
        return new CuttingBoardBlockEntity(BlockPos.ZERO, Blocks.OAK_SLAB.defaultBlockState());
    }

    @Test
    void cutsAndPoundsTrackIndependentlyAndResetOnItemChange() {
        CuttingBoardBlockEntity board = newBoard();
        assertEquals(0, board.cuts());
        assertEquals(0, board.pounds());
        assertTrue(board.item().isEmpty());

        board.setItem(new ItemStack(Items.BEEF), Direction.NORTH);
        assertEquals(0, board.cuts());
        assertEquals(0, board.pounds());
        assertEquals(Direction.NORTH, board.itemFacing());

        board.setPounds(1);
        assertEquals(1, board.pounds());
        assertEquals(0, board.cuts(), "捣一下不得污染 cuts 计数");

        board.setCuts(2);
        assertEquals(2, board.cuts());
        assertEquals(1, board.pounds(), "切两刀不得污染 pounds 计数");

        board.setPounds(2);
        assertEquals(2, board.pounds());
        assertEquals(2, board.cuts());

        // 换放新食材或清空时，切与捣次数必须同时清零
        board.setItem(new ItemStack(Items.POTATO), Direction.EAST);
        assertEquals(0, board.cuts());
        assertEquals(0, board.pounds());
        assertEquals(Direction.EAST, board.itemFacing());

        board.setCuts(3);
        board.setPounds(1);
        board.setItem(ItemStack.EMPTY);
        assertEquals(0, board.cuts());
        assertEquals(0, board.pounds());
    }

    @Test
    void saveAndLoadPersistCutsAndPoundsAndDefaultMissingLegacyPoundsToZero() {
        CuttingBoardBlockEntity savedBoard = newBoard();
        savedBoard.setItem(new ItemStack(Items.BEEF), Direction.WEST);
        savedBoard.setCuts(2);
        savedBoard.setPounds(1);

        CompoundTag savedTag = new CompoundTag();
        savedBoard.saveAdditional(savedTag);
        assertTrue(savedTag.contains("Cuts"));
        assertTrue(savedTag.contains("Pounds"));
        assertEquals(2, savedTag.getInt("Cuts"));
        assertEquals(1, savedTag.getInt("Pounds"));

        CuttingBoardBlockEntity reloaded = newBoard();
        reloaded.load(savedTag);
        assertTrue(reloaded.item().is(Items.BEEF));
        assertEquals(2, reloaded.cuts());
        assertEquals(1, reloaded.pounds());
        assertEquals(Direction.WEST, reloaded.itemFacing());

        // 模拟没有 Pounds 字段的旧档：缺省 Pounds 必须按 0 读入
        CompoundTag legacyTag = savedTag.copy();
        legacyTag.remove("Pounds");
        CuttingBoardBlockEntity legacyLoaded = newBoard();
        legacyLoaded.setPounds(2);
        legacyLoaded.load(legacyTag);
        assertEquals(2, legacyLoaded.cuts());
        assertEquals(0, legacyLoaded.pounds(), "旧档缺少 Pounds 字段时应默认读作 0");
    }

    @Test
    void poundingDisplayStagesMatchCutGateThreshold() {
        ItemStack raw = new ItemStack(Items.BEEF);
        ItemStack stage1 = new ItemStack(Items.MUTTON);
        ItemStack stage2 = new ItemStack(Items.RABBIT);
        PoundingRecipe recipe = new PoundingRecipe(Ingredient.of(Items.BEEF), new ItemStack(Items.SLIME_BALL, 3), 3, stage1, stage2);

        assertTrue(recipe.display(raw, 0).is(Items.BEEF));
        assertTrue(recipe.display(raw, 1).is(Items.MUTTON));
        assertTrue(recipe.display(raw, 2).is(Items.RABBIT));
    }
}
