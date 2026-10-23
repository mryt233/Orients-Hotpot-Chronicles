// 0.1.2 的 1.20.1 Fabric 接口迁移：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 一格里摆着的若干个盘子：每个盘子记它是哪种（材质 + 大小）、中心位置（像素）、长边方向和里面的菜。
 * 盘子之间不能重叠，所以一格能摆几个由盘子大小自然决定（小碟 5 个、长盘 2 个、方盘 1 个，也能混着摆）。
 */
public class PlateHolderBlockEntity extends BlockEntity {
    /** 摆着的一个盘子。 */
    public static final class Placed {
        public final PlateBlock plate;
        public double x;
        public double z;
        public Direction.Axis axis;
        public final DefaultedList<ItemStack> items;
        /** 整件摆上来的东西（汤碗、果橙多、空瓶；r99）：拿起来原样还回去；空 = 盘子 / 装菜的碗。 */
        public ItemStack self = ItemStack.EMPTY;

        Placed(PlateBlock plate, double x, double z, Direction.Axis axis) {
            this.plate = plate;
            this.x = x;
            this.z = z;
            this.axis = axis;
            this.items = DefaultedList.ofSize(plate.size().capacity(), ItemStack.EMPTY);
        }

        public double halfX() {
            return (axis == Direction.Axis.Z ? plate.size().width() : plate.size().length()) / 2;
        }

        public double halfZ() {
            return (axis == Direction.Axis.Z ? plate.size().length() : plate.size().width()) / 2;
        }

        boolean overlaps(double ox, double oz, double hx, double hz) {
            return Math.abs(x - ox) < halfX() + hx - 1e-6 && Math.abs(z - oz) < halfZ() + hz - 1e-6;
        }

        double distanceTo(double px, double pz) {
            double dx = Math.max(0, Math.abs(px - x) - halfX());
            double dz = Math.max(0, Math.abs(pz - z) - halfZ());
            return Math.sqrt(dx * dx + dz * dz);
        }

        /** 放第一个空格，满了返回 false。 */
        public boolean add(ItemStack single) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isEmpty()) {
                    items.set(i, single.copyWithCount(1));
                    return true;
                }
            }
            return false;
        }

        /** 拿走最后放进去的那一样。 */
        public ItemStack takeLast() {
            for (int i = items.size() - 1; i >= 0; i--) {
                if (!items.get(i).isEmpty()) {
                    ItemStack out = items.get(i);
                    items.set(i, ItemStack.EMPTY);
                    return out;
                }
            }
            return ItemStack.EMPTY;
        }

        /** 能不能往里放菜：汤碗、瓶子不行。 */
        public boolean holdsFood() {
            return self.isEmpty();
        }

        /** 变回手里的盘子物品（带着菜）；碗变回原版的碗，整件摆上来的原样还回去。 */
        public ItemStack toItem() {
            if (!self.isEmpty()) {
                return self.copy();
            }
            ItemStack stack = plate instanceof BowlBlock ? new ItemStack(net.minecraft.item.Items.BOWL) : new ItemStack(plate);
            ItemData.setContents(stack, items);   // 1.20.1 移植：存 NBT（Claude 改）
            PlateItem.syncPlateModel(stack);
            return stack;
        }
    }

    private final List<Placed> plates = new ArrayList<>();
    private VoxelShape shape;
    /** 盘里的菜高出盘子多少像素（选中框 / 碰撞算上它）。 */
    private static final double FOOD_H = 2.5;

    public PlateHolderBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.PLATE_HOLDER_BE, pos, state);
    }

    public List<Placed> plates() {
        return plates;
    }

    /**
     * 在点的位置附近摆一个盘子：先把中心放在点的地方（不出格），重叠了就找最近的空位（0.5 像素一档）。
     * 找不到返回 false。
     */
    public boolean tryAdd(PlateBlock plate, Direction.Axis axis, double hitX, double hitZ, ItemStack fromItem) {
        return tryAdd(plate, axis, hitX, hitZ, fromItem, false);
    }

    /** keepSelf：整件摆上来（汤碗、果橙多），记着原来那件物品。 */
    public boolean tryAdd(PlateBlock plate, Direction.Axis axis, double hitX, double hitZ, ItemStack fromItem,
                          boolean keepSelf) {
        Placed p = new Placed(plate, 8, 8, axis);
        double hx = p.halfX(), hz = p.halfZ();
        List<double[]> spots = new ArrayList<>();
        for (double x = hx; x <= 16 - hx + 1e-6; x += 0.5) {
            for (double z = hz; z <= 16 - hz + 1e-6; z += 0.5) {
                spots.add(new double[]{x, z});
            }
        }
        spots.sort(Comparator.comparingDouble(s -> (s[0] - hitX) * (s[0] - hitX) + (s[1] - hitZ) * (s[1] - hitZ)));
        for (double[] s : spots) {
            if (plates.stream().noneMatch(o -> o.overlaps(s[0], s[1], hx, hz))) {
                p.x = s[0];
                p.z = s[1];
                if (keepSelf) {
                    p.self = fromItem.copyWithCount(1);
                } else {
                    ItemData.copyContentsTo(fromItem, p.items);
                }
                plates.add(p);
                changed();
                return true;
            }
        }
        return false;
    }

    /** 准星点到的那个盘子（点在盘子上，或离盘子边不到 2 像素的最近一个）。 */
    public Placed at(double px, double pz) {
        Placed best = null;
        double bestD = 2.0;
        for (Placed p : plates) {
            double d = p.distanceTo(px, pz);
            if (d <= bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    public void remove(Placed p) {
        plates.remove(p);
        changed();
    }

    public void changed() {
        shape = null;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    /** 一个盘子（连盘里的菜）的形状，选中框只框准星对着的那个时用。 */
    public static VoxelShape partShape(Placed p, double lift) {
        double top = p.plate.size().height();
        if (p.items.stream().anyMatch(i -> !i.isEmpty())) {
            top += FOOD_H;
        }
        return Block.createCuboidShape(p.x - p.halfX(), lift, p.z - p.halfZ(), p.x + p.halfX(), lift + top, p.z + p.halfZ());
    }

    /** 所有盘子的形状拼起来（跟着桌面高低整体上下移）。 */
    public VoxelShape shape(double lift) {
        if (shape == null) {
            VoxelShape s = VoxelShapes.empty();
            for (Placed p : plates) {
                double top = p.plate.size().height();
                if (p.items.stream().anyMatch(i -> !i.isEmpty())) {
                    top += FOOD_H;   // 盘里有菜：菜高出盘沿，准星对着菜也要算选中这个盘子（2026-10-23，Claude）
                }
                s = VoxelShapes.union(s, Block.createCuboidShape(p.x - p.halfX(), lift, p.z - p.halfZ(),
                    p.x + p.halfX(), lift + top, p.z + p.halfZ()));
            }
            shape = s.isEmpty() ? Block.createCuboidShape(6, lift, 6, 10, lift + 1, 10) : s.simplify();
        }
        return shape;
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList list = new NbtList();
        for (Placed p : plates) {
            NbtCompound c = new NbtCompound();
            c.putString("Plate", Registries.BLOCK.getId(p.plate).toString());
            c.putDouble("X", p.x);
            c.putDouble("Z", p.z);
            c.putString("Axis", p.axis.asString());
            Inventories.writeNbt(c, p.items);
            if (!p.self.isEmpty()) {
                c.put("Self", p.self.writeNbt(new NbtCompound()));
            }
            list.add(c);
        }
        nbt.put("Plates", list);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        plates.clear();
        shape = null;
        NbtList list = nbt.getList("Plates", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            if (!(Registries.BLOCK.get(new Identifier(c.getString("Plate"))) instanceof PlateBlock plate)) {
                continue;
            }
            Direction.Axis axis = "z".equals(c.getString("Axis")) ? Direction.Axis.Z : Direction.Axis.X;
            Placed p = new Placed(plate, c.getDouble("X"), c.getDouble("Z"), axis);
            Inventories.readNbt(c, p.items);
            if (c.contains("Self")) {
                p.self = ItemStack.fromNbt(c.getCompound("Self"));
            }
            plates.add(p);
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
