package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

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
        public final NonNullList<ItemStack> items;
        /** 整件摆上来的东西（汤碗、橙子饮料、空瓶；r99）：拿起来原样还回去；空 = 盘子 / 装菜的碗。 */
        public ItemStack self = ItemStack.EMPTY;
        /** 架在碗沿 / 盘沿上的筷子（连夹着的菜；2026-10-27 用户，新增）：拿筷子 Shift+右键碗、盘子架上去，空手右键拿回来。 */
        public ItemStack chopsticks = ItemStack.EMPTY;

        /** 能不能架筷子：碗（空碗、汤碗、装菜的碗、蘸料碗）和盘子能，瓶子、杯子不能。 */
        public boolean restsChopsticks() {
            return !(plate instanceof DrinkBlock) && !(plate instanceof CupBlock);
        }

        Placed(PlateBlock plate, double x, double z, Direction.Axis axis) {
            this.plate = plate;
            this.x = x;
            this.z = z;
            this.axis = axis;
            this.items = NonNullList.withSize(plate.size().capacity(), ItemStack.EMPTY);
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
            ItemStack stack = plate instanceof BowlBlock ? new ItemStack(net.minecraft.world.item.Items.BOWL) : new ItemStack(plate);
            if (items.stream().anyMatch(s -> !s.isEmpty())) {
                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            }
            PlateItem.syncPlateModel(stack);
            return stack;
        }
    }

    private final List<Placed> plates = new ArrayList<>();
    private VoxelShape shape;
    /** 盘里的菜高出盘子多少像素（选中框 / 碰撞算上它）。 */
    private static final double FOOD_H = 2.5;

    public PlateHolderBlockEntity(BlockPos worldPosition, BlockState state) {
        super(HotPotContent.PLATE_HOLDER_BE, worldPosition, state);
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

    /** keepSelf：整件摆上来（汤碗、橙子饮料），记着原来那件物品。 */
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
                    fromItem.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(p.items);
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
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** 一个盘子（连盘里的菜）的形状，选中框只框准星对着的那个时用。 */
    /** 摆着的这件有多高（像素）：一般按盘子大小；之晗可乐（原名 之晗可乐，2026-10-26 用户改名）的玻璃汽水瓶只有 11.75（2026-10-26）。 */
    public static double heightOf(Placed p) {
        DrinkKind kind = p.self.getItem() instanceof DrinkItem d ? d.kind()
            : p.self.getItem() instanceof EmptyBottleItem e ? e.kind() : null;
        return kind == null ? p.plate.size().height() : kind.bottleHeight();
    }

    public static VoxelShape partShape(Placed p, double lift) {
        double top = heightOf(p);
        if (p.items.stream().anyMatch(i -> !i.isEmpty())) {
            top += FOOD_H;
        }
        return Block.box(p.x - p.halfX(), lift, p.z - p.halfZ(), p.x + p.halfX(), lift + top, p.z + p.halfZ());
    }

    /** 所有盘子的形状拼起来（跟着桌面高低整体上下移）。 */
    public VoxelShape shape(double lift) {
        if (shape == null) {
            VoxelShape s = Shapes.empty();
            for (Placed p : plates) {
                double top = heightOf(p);
                if (p.items.stream().anyMatch(i -> !i.isEmpty())) {
                    top += FOOD_H;   // 盘里有菜：菜高出盘沿，准星对着菜也要算选中这个盘子（2026-10-23）
                }
                s = Shapes.or(s, Block.box(p.x - p.halfX(), lift, p.z - p.halfZ(),
                    p.x + p.halfX(), lift + top, p.z + p.halfZ()));
            }
            shape = s.isEmpty() ? Block.box(6, lift, 6, 10, lift + 1, 10) : s.optimize();
        }
        return shape;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        ListTag list = new ListTag();
        for (Placed p : plates) {
            CompoundTag c = new CompoundTag();
            c.putString("Plate", BuiltInRegistries.BLOCK.getKey(p.plate).toString());
            c.putDouble("X", p.x);
            c.putDouble("Z", p.z);
            c.putString("Axis", p.axis.getSerializedName());
            ContainerHelper.saveAllItems(c, p.items, registries);
            if (!p.self.isEmpty()) {
                c.put("Self", p.self.save(registries));
            }
            if (!p.chopsticks.isEmpty()) {
                c.put("Chopsticks", p.chopsticks.save(registries));
            }
            list.add(c);
        }
        nbt.put("Plates", list);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        plates.clear();
        shape = null;
        ListTag list = nbt.getList("Plates", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (!(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(c.getString("Plate"))) instanceof PlateBlock plate)) {
                continue;
            }
            Direction.Axis axis = "z".equals(c.getString("Axis")) ? Direction.Axis.Z : Direction.Axis.X;
            Placed p = new Placed(plate, c.getDouble("X"), c.getDouble("Z"), axis);
            ContainerHelper.loadAllItems(c, p.items, registries);
            if (c.contains("Self")) {
                ItemStack parsed = ItemStack.parseOptional(registries, c.getCompound("Self"));
                p.self = parsed == null ? ItemStack.EMPTY : parsed;
            }
            if (c.contains("Chopsticks")) {
                ItemStack parsed = ItemStack.parseOptional(registries, c.getCompound("Chopsticks"));
                p.chopsticks = parsed == null ? ItemStack.EMPTY : parsed;
            }
            plates.add(p);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}