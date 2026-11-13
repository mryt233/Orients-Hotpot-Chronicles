package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.compat.RegistryFriendlyByteBuf;
import cn.blockforge.orient.framework.compat.StreamCodec;
import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/**
 * 筷子上夹着的那一样食物的组件值。
 *
 * <p>原来 {@code held_food} 直接把 {@link ItemStack} 当组件值存。Fabric 不校验这没问题，
 * NeoForge 的 {@code CommonHooks.validateComponent} 要求组件值实现 equals/hashCode 且不可变
 * （ItemStack 不满足），一夹菜就抛 {@code IllegalArgumentException}。照原版
 * {@code ChargedProjectiles} 的做法包一层：存单份拷贝、读取时再拷一份出去，
 * equals/hashCode 走内容等价与 NBT 哈希，持久化编码仍是原来的
 * ItemStack JSON，旧存档不受影响。</p>
 * 作者：mryt233
 */
public final class HeldFood {
    public static final HeldFood EMPTY = new HeldFood(ItemStack.EMPTY);
    public static final Codec<HeldFood> CODEC = ItemStack.CODEC.xmap(HeldFood::new, HeldFood::stack);
    public static final StreamCodec<RegistryFriendlyByteBuf, HeldFood> STREAM_CODEC =
        StreamCodec.of((buffer, food) -> buffer.writeItem(food.stack()), buffer -> new HeldFood(buffer.readItem()));

    private final ItemStack stack;

    private HeldFood(ItemStack stack) {
        this.stack = stack;
    }

    /** 存进组件：拷一份、数量固定 1（空栈直接用 EMPTY）。 */
    public static HeldFood of(ItemStack stack) {
        return stack.isEmpty() ? EMPTY : new HeldFood(stack.copyWithCount(1));
    }

    /** 组件里夹的菜（每次给拷贝，防止外部改动组件内部状态）。 */
    public ItemStack stack() {
        return stack.copy();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof HeldFood food && ItemStack.matches(this.stack, food.stack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack.getItem(), stack.getCount(), stack.getTag());
    }

    @Override
    public String toString() {
        return "HeldFood[" + stack + "]";
    }
}
