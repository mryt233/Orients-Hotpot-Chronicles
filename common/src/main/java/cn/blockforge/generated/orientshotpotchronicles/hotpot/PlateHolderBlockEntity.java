package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.minecraft.world.item.Items;

// mryt233 改（2026-11-11）：第三批 3G 存读档改走适配小包，键名与结构不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreChildList;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreReader;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreWriter;

import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
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
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public class PlateHolderBlockEntity extends SyncedBlockEntity {
    /** 摆着的一个盘子。 */
    public static final class Placed {
        public final PlateBlock plate;
        public double x;
        public double z;
        public Direction.Axis axis;
        // mryt233 改（r146e）：筷尖四向与碗沿角度分别存档，角度遵循玩家 yaw（0=南）。
        public Direction facing;
        public float chopsticksYaw;
        public final NonNullList<ItemStack> items;
        /** 整件摆上来的东西（汤碗、橙子饮料、空瓶；r99）：拿起来原样还回去；空 = 盘子 / 装菜的碗。 */
        public ItemStack self = ItemStack.EMPTY;
        /** 架在碗沿 / 盘沿上的筷子（连夹着的菜；2026-10-27 用户，新增）：拿筷子 Shift+右键碗、盘子架上去，空手右键拿回来。 */
        public ItemStack chopsticks = ItemStack.EMPTY;

        /**
         * 能不能架筷子：碗（空碗、汤碗、装菜的碗、蘸料碗）和盘子能，瓶子、杯子不能；
         * 已经摆在桌上的筷子本身（{@link ChopstickRestBlock}）也不能再叠架第二双（r136b，2026-11-05 用户）。
         */
        public boolean restsChopsticks() {
            // mryt233 改（r146a，2026-11-08）：芝麻酱 / 蚝油瓶和饮料瓶、杯子一样，瓶口太小，架不了筷子
            return !(plate instanceof DrinkBlock) && !(plate instanceof CupBlock)
                && !(plate instanceof ChopstickRestBlock) && !(plate instanceof SauceBottleBlock);
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

        /**
         * 能不能往里放菜：汤碗、瓶子不行。
         * 摆在桌上的筷子（{@link ChopstickRestBlock}）self 里存的就是这双 bamboo_chopsticks、不为空，
         * 所以这里天然返回 false，不会往筷子上塞盘子菜（r136b，2026-11-05 用户）。
         */
        public boolean holdsFood() {
            return self.isEmpty();
        }

        /** 变回手里的盘子物品（带着菜）；碗变回原版的碗，整件摆上来的原样还回去。 */
        public ItemStack toItem() {
            if (!self.isEmpty()) {
                return self.copy();
            }
            ItemStack stack = plate instanceof BowlBlock ? new ItemStack(Items.BOWL) : new ItemStack(plate);
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
        return tryAdd(plate, axis, hitX, hitZ, fromItem, keepSelf,
            axis == Direction.Axis.Z ? Direction.SOUTH : Direction.EAST);
    }

    /** mryt233 改：只有桌面竹筷的占地跟随筷尖朝向，普通餐具保持旧 axis。 */
    public boolean tryAdd(PlateBlock plate, Direction.Axis axis, double hitX, double hitZ, ItemStack fromItem,
                          boolean keepSelf, Direction facing) {
        if (facing == null || !facing.getAxis().isHorizontal()) facing = Direction.EAST;
        if (plate instanceof ChopstickRestBlock) axis = facing.getAxis();
        Placed p = new Placed(plate, 8, 8, axis);
        p.facing = facing;
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
        double bestCenterD = Double.POSITIVE_INFINITY;
        for (Placed p : plates) {
            double d = p.distanceTo(px, pz);
            double dx = px - p.x, dz = pz - p.z;
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
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** mryt233 改（r146e）：筷架最高 1.6，竹条最高约 1.74 像素；夹菜另计高度。 */
    public static final double TABLE_CHOPSTICKS_H = 2.0; // mryt233 改：最高竹条约 1.74 像素，留到 2。

    /**
     * 摆着的这件有多高（像素）：一般按盘子大小；之晗可乐（原名 之晗可乐，2026-10-26 用户改名）的玻璃汽水瓶只有 11.75（2026-10-26）；
     * 直接摆在桌上的竹筷（{@link ChopstickRestBlock}）按筷架 + 筷子算（r136b，2026-11-05 用户）。
     */
    public static double heightOf(Placed p) {
        if (p.plate instanceof ChopstickRestBlock) {
            return TABLE_CHOPSTICKS_H + (Chopsticks.held(p.self).isEmpty() ? 0 : FOOD_H);
        }
        // mryt233 改（r146a，2026-11-08）：摆在桌上的芝麻酱 / 蚝油瓶按自己那种瓶子的高度算
        // （芝麻酱矮罐 6 像素、蚝油细高瓶 8 像素，见 SauceBottleKind#bottleHeight）。
        if (p.plate instanceof SauceBottleBlock) {
            return SauceBottleBlock.heightOf(SauceBottleItem.kindOf(p.self));
        }
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

    // mryt233 改（2026-11-11）：第三批 3G 存读档改走适配小包，键名、结构与兜底值不变。
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        BeIo.write(nbt, registries, w -> {
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
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        BeIo.read(nbt, registries, r -> {
            plates.clear();
            shape = null;
            for (StoreReader c : r.children("Plates")) {
                if (!(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(c.getString("Plate", "")))
                        instanceof PlateBlock plate)) {
                    continue;
                }
                Direction.Axis axis = "z".equals(c.getString("Axis", "")) ? Direction.Axis.Z : Direction.Axis.X;
                Placed p = new Placed(plate, c.getDouble("X", 0.0), c.getDouble("Z", 0.0), axis);
                // mryt233 改：旧档缺字段或字段损坏，使用构造时按 axis 得到的默认值。
                Direction facing = Direction.byName(c.getString("Facing", ""));
                if (facing != null && facing.getAxis().isHorizontal()) p.facing = facing;
                if (plate instanceof ChopstickRestBlock) p.axis = p.facing.getAxis();
                if (c.hasNumber("ChopsticksYaw")) {
                    float yaw = c.getFloat("ChopsticksYaw", 0F);
                    if (Float.isFinite(yaw)) p.chopsticksYaw = ((yaw % 360F) + 360F) % 360F;
                }
                c.loadItems(p.items);
                p.self = c.getItem("Self");
                p.chopsticks = c.getItem("Chopsticks");
                plates.add(p);
            }
        });
    }


    /** 保留公开入口，实际更新格式由公共基类提供。 */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return super.getUpdatePacket(); }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return super.getUpdateTag(registries); }
}
