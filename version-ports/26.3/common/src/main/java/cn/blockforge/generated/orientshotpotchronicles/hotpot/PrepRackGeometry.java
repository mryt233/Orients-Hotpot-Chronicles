// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 架体内的展示净空与槽中心，交互、描边、渲染共享。局部 x/z 以方块中心为原点。 */
public final class PrepRackGeometry {
    public record Cell(double x, double y, double z, double halfWidth, double halfHeight, double halfDepth) {
        double distance(Vec3 p) { return p.distanceToSqr(x,y,z); }
        AABB box() { return new AABB(x-halfWidth,y-halfHeight,z-halfDepth,x+halfWidth,y+halfHeight,z+halfDepth); }
    }
    private static final Cell[] MULTI = new Cell[6], DESK = new Cell[4], WALL = new Cell[4], CEILING = new Cell[4];
    static {
        for (int i=0; i<MULTI.length; i++) {
            double base = new double[]{1.65, 7.65, 13.15}[i/2];
            double top = new double[]{6.3, 11.85, 15.9}[i/2];
            MULTI[i] = pixels((i%2==0 ? -3.15:3.15), (base+top)/2, .2,
                2.85, (top-base)/2, 4.85);
        }
        for (int i=0; i<4; i++) {
            DESK[i] = pixels(i%2==0 ? -3.15:3.15, i<2 ? 3.5:8.1, i<2 ? 2.7:-2.1,
                2.85, 1.75, i<2 ? 2.30:2.35);
            WALL[i] = pixels(-4.5+3*i,7.9,-4.55,1.35,1.62,1.35);
            // mryt233 改：铁钩尖端 8.8 px，槽顶在 8.9 px，保留与上方木板的净空。
            CEILING[i] = pixels(-4.5+3*i,7.70,0,1.35,1.20,1.65);
        }
    }
    private PrepRackGeometry() {}
    private static Cell pixels(double x,double y,double z,double w,double h,double d) {
        return new Cell(x/16,y/16,z/16,w/16,h/16,d/16);
    }
    public static Cell cell(PrepRackBlock.Kind kind, PrepRackBlock.Mount mount, int index) {
        return switch (kind) {
            case MULTI_TIER -> MULTI[index];
            case DESKTOP -> DESK[index];
            case HANGING -> (mount == PrepRackBlock.Mount.CEILING ? CEILING : WALL)[index];
        };
    }

    public static Vec3 local(Direction facing, BlockPos pos, double lift, Vec3 v) {
        double x=v.x-pos.getX()-.5, y=v.y-pos.getY()-lift, z=v.z-pos.getZ()-.5;
        return switch(facing) {
            case NORTH -> new Vec3(-x,y,-z);
            case EAST -> new Vec3(-z,y,x);
            case WEST -> new Vec3(z,y,-x);
            default -> new Vec3(x,y,z);
        };
    }

    public static VoxelShape shape(Cell c, Direction facing, double lift) {
        double x=c.x, z=c.z, w=c.halfWidth, d=c.halfDepth;
        switch(facing) {
            case NORTH -> { x=-c.x; z=-c.z; }
            case EAST -> { x=c.z; z=-c.x; w=c.halfDepth; d=c.halfWidth; }
            case WEST -> { x=-c.z; z=c.x; w=c.halfDepth; d=c.halfWidth; }
        }
        return Shapes.box(.5+x-w,lift+c.y-c.halfHeight,.5+z-d,.5+x+w,lift+c.y+c.halfHeight,.5+z+d);
    }
}
