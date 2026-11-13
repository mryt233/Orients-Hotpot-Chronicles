// mryt233 改：26.2 原版 API；第三批 3F/3G 合并与存读档适配。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreChildList;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreReader;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 一格里摆着的若干个盘子：每个盘子记它是哪种、中心位置、长边方向和里面的菜。
 */
public class PlateHolderBlockEntity extends SyncedBlockEntity {
    public static final class Placed {
        public final PlateBlock plate;
        public double x;
        public double z;
        public Direction.Axis axis;
        public Direction facing;
        public float chopsticksYaw;
        public final NonNullList<ItemStack> items;
        public ItemStack self = ItemStack.EMPTY;
        public ItemStack chopsticks = ItemStack.EMPTY;

        public boolean restsChopsticks() {
            return plate.restsChopsticks(this);
        }

        Placed(PlateBlock plate, double x, double z, Direction.Axis axis) {
            this.plate = plate;
            this.x = x;
            this.z = z;
            this.axis = axis;
            this.facing = axis == Direction.Axis.Z ? Direction.SOUTH : Direction.EAST;
            this.chopsticksYaw = axis == Direction.Axis.Z ? 135F : 45F;
            this.items = NonNullList.withSize(plate.size().capacity(), ItemStack.EMPTY);
        }

        public double halfX() {
            return (axis == Direction.Axis.Z ? plate.footprintWidth() : plate.footprintLength()) / 2;
        }

        public double halfZ() {
            return (axis == Direction.Axis.Z ? plate.footprintLength() : plate.footprintWidth()) / 2;
        }

        boolean overlaps(double ox, double oz, double hx, double hz) {
            return Math.abs(x - ox) < halfX() + hx - 1e-6 && Math.abs(z - oz) < halfZ() + hz - 1e-6;
        }

        double distanceTo(double px, double pz) {
            double dx = Math.max(0, Math.abs(px - x) - halfX());
            double dz = Math.max(0, Math.abs(pz - z) - halfZ());
            return Math.sqrt(dx * dx + dz * dz);
        }

        public boolean add(ItemStack single) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isEmpty()) {
                    items.set(i, single.copyWithCount(1));
                    return true;
                }
            }
            return false;
        }

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

        public boolean holdsFood() {
            return plate.holdsFood(this);
        }

        public ItemStack toItem() {
            return plate.toTableItem(this);
        }
    }

    private final List<Placed> plates = new ArrayList<>();
    private VoxelShape shape;
    static final double FOOD_H = 2.5;

    public PlateHolderBlockEntity(BlockPos worldPosition, BlockState state) {
        super(HotPotContent.PLATE_HOLDER_BE, worldPosition, state);
    }

    public List<Placed> plates() {
        return plates;
    }

    public boolean tryAdd(PlateBlock plate, Direction.Axis axis, double hitX, double hitZ, ItemStack fromItem) {
        return tryAdd(plate, axis, hitX, hitZ, fromItem, false);
    }

    public boolean tryAdd(PlateBlock plate, Direction.Axis axis, double hitX, double hitZ, ItemStack fromItem,
                          boolean keepSelf) {
        return tryAdd(plate, axis, hitX, hitZ, fromItem, keepSelf,
            axis == Direction.Axis.Z ? Direction.SOUTH : Direction.EAST);
    }

    public boolean tryAdd(PlateBlock plate, Direction.Axis axis, double hitX, double hitZ, ItemStack fromItem,
                          boolean keepSelf, Direction facing) {
        if (facing == null || !facing.getAxis().isHorizontal()) {
            facing = Direction.EAST;
        }
        Placed p = new Placed(plate, 8, 8, axis);
        p.facing = facing;
        plate.onLoadedOrAdded(p, facing);
        double hx = p.halfX();
        double hz = p.halfZ();
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
                    p.self = TableObjects.prepareForTable(fromItem).copyWithCount(1);
                } else {
                    fromItem.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(p.items);
                }
                plates.add(p);
                changed();
                return true;
            }
        }
        return false;
    }

    public Placed at(double px, double pz) {
        Placed best = null;
        double bestD = 2.0;
        double bestCenterD = Double.POSITIVE_INFINITY;
        for (Placed p : plates) {
            double d = p.distanceTo(px, pz);
            double dx = px - p.x;
            double dz = pz - p.z;
            double centerD = dx * dx + dz * dz;
            if (d < bestD || (d == bestD && centerD < bestCenterD)) {
                bestD = d;
                bestCenterD = centerD;
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
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static final double TABLE_CHOPSTICKS_H = 2.0;

    public static double heightOf(Placed p) {
        return p.plate.tableHeight(p);
    }

    public static VoxelShape partShape(Placed p, double lift) {
        double top = heightOf(p);
        if (p.items.stream().anyMatch(i -> !i.isEmpty())) {
            top += FOOD_H;
        }
        return Block.box(p.x - p.halfX(), lift, p.z - p.halfZ(), p.x + p.halfX(), lift + top, p.z + p.halfZ());
    }

    public VoxelShape shape(double lift) {
        if (shape == null) {
            VoxelShape s = Shapes.empty();
            for (Placed p : plates) {
                double top = heightOf(p);
                if (p.items.stream().anyMatch(i -> !i.isEmpty())) {
                    top += FOOD_H;
                }
                s = Shapes.or(s, Block.box(p.x - p.halfX(), lift, p.z - p.halfZ(),
                    p.x + p.halfX(), lift + top, p.z + p.halfZ()));
            }
            shape = s.isEmpty() ? Block.box(6, lift, 6, 10, lift + 1, 10) : s.optimize();
        }
        return shape;
    }

    @Override
    protected void saveAdditional(ValueOutput nbt) {
        super.saveAdditional(nbt);
        BeIo.write(nbt, w -> {
            StoreChildList list = w.childList("Plates");
            for (Placed p : plates) {
                StoreWriter c = list.addChild();
                c.putString("Plate", BuiltInRegistries.BLOCK.getKey(p.plate).toString());
                c.putDouble("X", p.x);
                c.putDouble("Z", p.z);
                c.putString("Axis", p.axis.getSerializedName());
                c.putString("Facing", p.facing.getSerializedName());
                c.putFloat("ChopsticksYaw", p.chopsticksYaw);
                c.putItems(p.items);
                c.putItem("Self", p.self);
                c.putItem("Chopsticks", p.chopsticks);
            }
        });
    }

    @Override
    protected void loadAdditional(ValueInput nbt) {
        super.loadAdditional(nbt);
        BeIo.read(nbt, r -> {
            plates.clear();
            shape = null;
            for (StoreReader c : r.children("Plates")) {
                if (!(BuiltInRegistries.BLOCK.getValue(Identifier.parse(c.getString("Plate", "")))
                        instanceof PlateBlock plate)) {
                    continue;
                }
                Direction.Axis axis = "z".equals(c.getString("Axis", "")) ? Direction.Axis.Z : Direction.Axis.X;
                Placed p = new Placed(plate, c.getDouble("X", 0.0), c.getDouble("Z", 0.0), axis);
                Direction facing = Direction.byName(c.getString("Facing", ""));
                if (facing != null && facing.getAxis().isHorizontal()) {
                    p.facing = facing;
                }
                plate.onLoadedOrAdded(p, p.facing);
                if (c.hasNumber("ChopsticksYaw")) {
                    float yaw = c.getFloat("ChopsticksYaw", 0F);
                    if (Float.isFinite(yaw)) {
                        p.chopsticksYaw = ((yaw % 360F) + 360F) % 360F;
                    }
                }
                c.loadItems(p.items);
                p.self = c.getItem("Self");
                p.chopsticks = c.getItem("Chopsticks");
                plates.add(p);
            }
        });
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return super.getUpdatePacket();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            for (Placed plate : plates) {
                Block.popResource(level, pos, plate.toItem());
                if (!plate.chopsticks.isEmpty()) {
                    Block.popResource(level, pos, plate.chopsticks);
                }
            }
            plates.clear();
        }
    }
}
