// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.network.PotBreakEffectPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 不启动世界的形状、坐标、采样与效果编码回归。 */
class BlockBreakSamplesTest {
    @BeforeAll static void bootstrap() { Opt3AttributesTest.bootstrapMinecraft(); }

    @Test void stovePotBoundsUseActualLowAndTallHeights() {
        var low = StoveBlock.potShapeFor(0, Direction.Axis.X).bounds();
        var tall = StoveBlock.potShapeFor(StoveBlock.TALL_LIFT, Direction.Axis.X).bounds();
        assertEquals(7.6 / 16, low.minY, 1e-9);
        assertEquals(1, low.maxY, 1e-9);
        assertEquals(17.6 / 16, tall.minY, 1e-9);
        assertEquals(26.0 / 16, tall.maxY, 1e-9);
    }

    @Test void handleAxesRotateWithoutChangingPotHeight() {
        var x = StoveBlock.potShapeFor(0, Direction.Axis.X);
        var z = StoveBlock.potShapeFor(0, Direction.Axis.Z);
        Vec3 westHandle = new Vec3(.01, 14.5 / 16, .5);
        assertTrue(x.toAabbs().stream().anyMatch(box -> box.contains(westHandle)));
        assertFalse(z.toAabbs().stream().anyMatch(box -> box.contains(westHandle)));
        assertEquals(x.bounds().minY, z.bounds().minY, 1e-9);
        assertEquals(x.bounds().maxY, z.bounds().maxY, 1e-9);
    }

    @Test void tallSamplesAreTranslatedNotClampedToBaseBlock() {
        var low = BlockBreakSamples.destroy(StoveBlock.potShapeFor(0, Direction.Axis.Z));
        var tall = BlockBreakSamples.destroy(StoveBlock.potShapeFor(10, Direction.Axis.Z));
        assertFalse(low.isEmpty());
        assertEquals(low.size(), tall.size());
        for (int i = 0; i < low.size(); i++) {
            assertEquals(low.get(i).position().x, tall.get(i).position().x, 1e-9);
            assertEquals(low.get(i).position().y + 10.0 / 16, tall.get(i).position().y, 1e-9);
            assertEquals(low.get(i).velocity(), tall.get(i).velocity());
            assertTrue(tall.get(i).position().y > 1);
        }
    }

    @Test void everyDestroySampleIsInsideActualPotNotBrickBase() {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            var shape = StoveBlock.potShapeFor(10, axis);
            for (var sample : BlockBreakSamples.destroy(shape)) {
                assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(sample.position())));
            }
        }
    }

    @Test void ordinaryCubeKeepsVanillaGridAndEmptyShapeEmitsNothing() {
        assertEquals(64, BlockBreakSamples.destroy(Shapes.block()).size());
        assertTrue(BlockBreakSamples.destroy(Shapes.empty()).isEmpty());
        assertThrows(UnsupportedOperationException.class,
            () -> BlockBreakSamples.destroy(Shapes.block()).clear());
    }

    @Test void dropPointsAreAbovePotRimsEvenAtNegativeWorldCoordinates() {
        BlockPos origin = new BlockPos(-17, -61, 23);
        for (double lift : new double[]{0, 10}) {
            var shape = StoveBlock.potShapeFor(lift, Direction.Axis.Z);
            Vec3 point = BlockBreakSamples.dropPoint(origin, shape);
            assertEquals(-16.5, point.x, 1e-9);
            assertEquals(23.5, point.z, 1e-9);
            assertEquals(origin.getY() + shape.bounds().maxY + .125, point.y, 1e-9);
        }
    }

    @Test void thinHitRegionsDoNotReverseRandomRanges() {
        AABB thin = new AABB(.1, 1.2, .2, .11, 1.21, .21);
        for (Direction face : Direction.values()) {
            Vec3 p = BlockBreakSamples.hit(thin, face, RandomSource.create(42));
            switch (face) {
                case UP -> assertEquals(thin.maxY + .1, p.y, 1e-9);
                case DOWN -> assertEquals(thin.minY - .1, p.y, 1e-9);
                case NORTH -> assertEquals(thin.minZ - .1, p.z, 1e-9);
                case SOUTH -> assertEquals(thin.maxZ + .1, p.z, 1e-9);
                case EAST -> assertEquals(thin.maxX + .1, p.x, 1e-9);
                case WEST -> assertEquals(thin.minX - .1, p.x, 1e-9);
            }
            if (face.getAxis() != Direction.Axis.X) assertTrue(p.x >= thin.minX && p.x <= thin.maxX);
            if (face.getAxis() != Direction.Axis.Y) assertTrue(p.y >= thin.minY && p.y <= thin.maxY);
            if (face.getAxis() != Direction.Axis.Z) assertTrue(p.z >= thin.minZ && p.z <= thin.maxZ);
        }
    }

    @Test void effectPayloadCopiesMutablePositionAndRoundTripsExactly() {
        var mutable = new BlockPos.MutableBlockPos(-4, 72, 8);
        var payload = new PotBreakEffectPayload(mutable, 37);
        mutable.move(0, 10, 0);
        assertEquals(new BlockPos(-4, 72, 8), payload.pos());
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            PotBreakEffectPayload.CODEC.encode(buffer, payload);
            assertEquals(payload, PotBreakEffectPayload.CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
