// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** EXP-REG 探针：注册窗口、持有器、循环引用的最小实验。作者：mryt233 */
public final class RegProbe {
    public static final Identifier BLOCK_ID = HotPot.id("probe_block");
    public static final Identifier ITEM_ID = HotPot.id("probe_item");
    public static final Identifier ITEM_W_ID = HotPot.id("probe_item_wrong_window");
    public static final Identifier BE_ID = HotPot.id("probe_be");

    public static Block block;
    public static Item item;
    public static BlockEntityType<ProbeBE> beType;
    /** 早期绑定：字段为 null 时取到 null。 */
    public static final Supplier<Item> earlySupplier = () -> item;
    /** 晚期绑定：按 id 从注册表查。 */
    public static final Supplier<Item> lateSupplier = () -> BuiltInRegistries.ITEM.getValue(ITEM_ID);
    public static final Supplier<Block> blockLookup = () -> BuiltInRegistries.BLOCK.getValue(BLOCK_ID);

    private RegProbe() {
    }

    public static void log(String message) {
        System.out.println("[EXP-REG] " + message);
    }

    public static void logError(String phase, Throwable t) {
        log(phase + " -> " + t.getClass().getName() + ": " + t.getMessage());
    }

    public static String describeItem(Item value) {
        return value == null ? "null" : BuiltInRegistries.ITEM.getKey(value).toString();
    }

    public static String describeBlock() {
        return BuiltInRegistries.BLOCK.containsKey(BLOCK_ID)
            ? BuiltInRegistries.BLOCK.getValue(BLOCK_ID).toString() : "missing";
    }

    public static BlockItem newProbeItem(Block target) {
        return new BlockItem(target, new Item.Properties());
    }

    /** 方块实体：构造时引用静态类型字段，用于验证「类型晚于方块」的循环。 */
    public static final class ProbeBE extends BlockEntity {
        public ProbeBE(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            super(beType, pos, state);
        }
    }

    /** 探针方块：创建方块实体。 */
    public static final class ProbeBlock extends Block
        implements net.minecraft.world.level.block.EntityBlock {
        public ProbeBlock() {
            super(Properties.of());
        }

        @Override
        public BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos,
                                           net.minecraft.world.level.block.state.BlockState state) {
            return new ProbeBE(pos, state);
        }
    }
}