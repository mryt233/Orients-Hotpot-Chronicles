// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 高灶台上半截即时同步与锅体命中坐标回归测试。作者：mryt233 */
class StoveTopSyncTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    private static BlockState stoveState(double lift) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        StoveBlock stove = (StoveBlock) unsafe.allocateInstance(StoveBlock.class);
        Field liftField = StoveBlock.class.getDeclaredField("lift");
        liftField.setAccessible(true);
        liftField.setDouble(stove, lift);
        Field propsField = BlockBehaviour.class.getDeclaredField("properties");
        propsField.setAccessible(true);
        propsField.set(stove, BlockBehaviour.Properties.of());
        StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(stove);
        builder.add(StoveBlock.LIT, StoveBlock.POT, StoveBlock.SOUP, StoveBlock.FACING, StoveBlock.POT_AXIS);
        return builder.create(Block::defaultBlockState, BlockState::new).any()
            .setValue(StoveBlock.LIT, false)
            .setValue(StoveBlock.POT, StovePot.NONE)
            .setValue(StoveBlock.SOUP, SoupVisual.NONE)
            .setValue(StoveBlock.FACING, Direction.NORTH)
            .setValue(StoveBlock.POT_AXIS, Direction.Axis.X);
    }

    @Test
    void belowEnumReflectsTallStovePotAndAxis() throws Exception {
        BlockState emptyTall = stoveState(StoveBlock.TALL_LIFT);
        assertEquals(StoveTopBlock.Below.EMPTY, StoveTopBlock.Below.of(emptyTall));

        BlockState attachedX = emptyTall
            .setValue(StoveBlock.POT, StovePot.COPPER_YUANYANG_S)
            .setValue(StoveBlock.POT_AXIS, Direction.Axis.X);
        assertEquals(StoveTopBlock.Below.POT_X, StoveTopBlock.Below.of(attachedX));

        BlockState attachedZ = attachedX.setValue(StoveBlock.POT_AXIS, Direction.Axis.Z);
        assertEquals(StoveTopBlock.Below.POT_Z, StoveTopBlock.Below.of(attachedZ));

        BlockState lowStove = stoveState(0.0).setValue(StoveBlock.POT, StovePot.COPPER_YUANYANG_S);
        assertEquals(StoveTopBlock.Below.NONE, StoveTopBlock.Below.of(lowStove));
    }

    @Test
    void tallStovePotHitRequiresUpperHalfY() throws Exception {
        BlockPos stovePos = new BlockPos(249, 4, 258);
        BlockState withPot = stoveState(StoveBlock.TALL_LIFT).setValue(StoveBlock.POT, StovePot.COPPER_YUANYANG_S);
        BlockHitResult brickHit = new BlockHitResult(new Vec3(249.5, 4.9, 258.5), Direction.UP, stovePos, false);
        assertFalse(StoveBlock.isPotHit(withPot, stovePos, brickHit), "clicking tall stove lower brick face must open stove, not hit pot");

        BlockHitResult topPotHit = new BlockHitResult(new Vec3(249.5, 5.6, 258.5), Direction.UP, stovePos.above(), false);
        assertTrue(StoveBlock.isPotHit(withPot, stovePos, topPotHit), "clicking tall_brick_stove_top upper half must hit pot");
    }

    @Test
    void attachAndRemovePotSyncTallTopImmediately() throws Exception {
        Path source = Path.of("src/main/java/cn/blockforge/generated/orientshotpotchronicles/hotpot/StoveBlockEntity.java");
        if (!Files.isRegularFile(source)) {
            source = Path.of("common/src/main/java/cn/blockforge/generated/orientshotpotchronicles/hotpot/StoveBlockEntity.java");
        }
        String text = Files.readString(source);
        String attachBody = text.substring(text.indexOf("public boolean attachPot("), text.indexOf("public void detachPot("));
        String removeBody = text.substring(text.indexOf("private void removePot()"), text.indexOf("public void breakPot("));
        assertTrue(attachBody.contains("syncTallTop("), "attachPot must sync tall stove top immediately");
        assertTrue(removeBody.contains("syncTallTop("), "removePot must sync tall stove top immediately");
        assertTrue(text.contains("stove.placeTop(level, worldPosition)"), "syncTallTop must invoke stove.placeTop");
    }
}
