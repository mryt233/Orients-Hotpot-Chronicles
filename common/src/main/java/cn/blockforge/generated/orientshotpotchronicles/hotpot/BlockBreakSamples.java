// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 原版破坏粒子网格的无世界采样器；坐标保持形状原点，不把高于一格的部件压回底格。 */
public final class BlockBreakSamples {
    public record Sample(Vec3 position, Vec3 velocity) { }

    private BlockBreakSamples() { }

    public static Vec3 dropPoint(net.minecraft.core.BlockPos origin, VoxelShape shape) {
        AABB box = shape.bounds();
        return new Vec3(origin.getX() + (box.minX + box.maxX) / 2,
            origin.getY() + box.maxY + .125, origin.getZ() + (box.minZ + box.maxZ) / 2);
    }

    public static List<Sample> destroy(VoxelShape shape) {
        List<Sample> samples = new ArrayList<>();
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            double dx = Math.min(1, x1 - x0);
            double dy = Math.min(1, y1 - y0);
            double dz = Math.min(1, z1 - z0);
            int nx = Math.max(2, Mth.ceil(dx / .25));
            int ny = Math.max(2, Mth.ceil(dy / .25));
            int nz = Math.max(2, Mth.ceil(dz / .25));
            for (int x = 0; x < nx; x++) {
                for (int y = 0; y < ny; y++) {
                    for (int z = 0; z < nz; z++) {
                        double u = (x + .5) / nx;
                        double v = (y + .5) / ny;
                        double w = (z + .5) / nz;
                        samples.add(new Sample(new Vec3(x0 + u * dx, y0 + v * dy, z0 + w * dz),
                            new Vec3(u - .5, v - .5, w - .5)));
                    }
                }
            }
        });
        return List.copyOf(samples);
    }

    /** 与原版持续挖掘同样每次一粒；窄部件的随机范围不反转。 */
    public static Vec3 hit(AABB box, Direction face, RandomSource random) {
        double x = inside(box.minX, box.maxX, random);
        double y = inside(box.minY, box.maxY, random);
        double z = inside(box.minZ, box.maxZ, random);
        return switch (face) {
            case DOWN -> new Vec3(x, box.minY - .1, z);
            case UP -> new Vec3(x, box.maxY + .1, z);
            case NORTH -> new Vec3(x, y, box.minZ - .1);
            case SOUTH -> new Vec3(x, y, box.maxZ + .1);
            case WEST -> new Vec3(box.minX - .1, y, z);
            case EAST -> new Vec3(box.maxX + .1, y, z);
        };
    }

    private static double inside(double min, double max, RandomSource random) {
        double inset = Math.min(.1, (max - min) / 2);
        return min + inset + random.nextDouble() * Math.max(0, max - min - 2 * inset);
    }
}
