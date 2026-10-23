// 0.1.2 的 1.20.1 Forge 接口迁移：。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/**
 * 蘸料台的存货（r103，用户定；2026-10-23 用户改成碗不固定，）：台上 9 个小碗，每个碗对应补货界面的一个原料格。
 * 哪个碗装什么不固定——空碗（格子里没原料、也没剩份数）放哪种原料就变成哪种调料，同一种调料也可以放好几个碗；
 * 碗里还有东西时只能补同一种。1 个原料 = 4 份，最多 16 个 = 64 份；打一次用 1 份，份数用完时自动从格子里化开 1 个原料。
 * 每个碗装的什么、有没有货同步给客户端，由 client/SauceStationRenderer 画碗里的调料面。
 * 碗的编号 = 阶梯上的位置：从最上层到最下层、每层从左到右（见 SauceStationBlock.aimed）。
 *
 */
public class SauceStationBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public static final int BOWLS = 9;
    public static final int PORTIONS_PER_RAW = 4;
    public static final int MAX_RAW = 16;

    private final SimpleInventory raw = new SimpleInventory(BOWLS) {
        @Override
        public boolean isValid(int slot, ItemStack stack) {
            return accepts(slot, stack);
        }

        @Override
        public int getMaxCountPerStack() {
            return MAX_RAW;
        }
    };
    private final int[] portions = new int[BOWLS];
    /** 每个碗已经化开的份数是哪种调料（Seasoning 序号，-1 = 没有）。 */
    private final int[] kinds = new int[BOWLS];
    /** 读存档时往格子里放东西，不要触发 changed（那时可能还在加载区块）。 */
    private boolean loading;

    public SauceStationBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.SAUCE_STATION_BE, pos, state);
        java.util.Arrays.fill(kinds, -1);
        raw.addListener(inv -> {
            if (!loading) {
                changed();
            }
        });
    }

    public SimpleInventory rawInventory() {
        return raw;
    }

    public int portions(int i) {
        return portions[i];
    }

    /** 第 i 个碗装的是哪种调料：格子里有原料看原料，没原料但还剩份数看剩的；空碗返回 null。 */
    public Seasoning kind(int i) {
        ItemStack stack = raw.getStack(i);
        if (!stack.isEmpty()) {
            return Seasoning.ofRaw(stack);
        }
        return portions[i] > 0 && kinds[i] >= 0 ? Seasoning.ALL[kinds[i]] : null;
    }

    /** 这个碗收不收这件原料：空碗什么调料的原料都收，有东西的碗只收同一种。 */
    public boolean accepts(int i, ItemStack stack) {
        Seasoning s = Seasoning.ofRaw(stack);
        Seasoning k = kind(i);
        return s != null && (k == null || k == s);
    }

    /** 这个碗一共还能打几次（已化开的份数 + 格子里的原料 × 4）。 */
    public int available(int i) {
        return portions[i] + raw.getStack(i).getCount() * PORTIONS_PER_RAW;
    }

    /** 从第 i 个碗用掉一份；没货返回 false。 */
    private boolean take(int i) {
        if (portions[i] == 0) {
            ItemStack stack = raw.getStack(i);
            if (stack.isEmpty()) {
                return false;
            }
            kinds[i] = Seasoning.ofRaw(stack).ordinal();
            stack.decrement(1);
            portions[i] += PORTIONS_PER_RAW;
        }
        portions[i]--;
        changed();
        return true;
    }

    /**
     * 拿碗 / 蘸料对着台上第 i 个小碗右键：往碗里加一份这个小碗里的调料。空碗变成一碗新的蘸料（9 口）；
     * 已经有这种的不重复加（用户：同一种一碗只能加一次）；加到吃了一半的碗里，剩余口数不变。
     */
    public void serve(PlayerEntity player, Hand hand, int i) {
        Seasoning s = kind(i);
        if (s == null || available(i) == 0) {
            player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.bowl_empty")), true);
            return;
        }
        ItemStack held = player.getStackInHand(hand);
        boolean sauce = held.getItem() instanceof DippingSauceItem;
        if (sauce && DippingSauceItem.contents(held).has(s)) {
            player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.sauce_has"), Text.translatable(s.nameKey())), true);
            return;
        }
        if (!take(i)) {
            return;
        }
        if (sauce) {
            ItemData.setSauce(held, DippingSauceItem.contents(held).with(s));
        } else {
            player.setStackInHand(hand, ItemUsage.exchangeStack(held, player, DippingSauceItem.create(s)));
        }
        world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 0.6F, 1.4F);
    }

    /** 存货变了：记下每个碗是哪种、存档、同步给客户端（台上的小碗跟着显示）。 */
    public void changed() {
        for (int i = 0; i < BOWLS; i++) {
            ItemStack stack = raw.getStack(i);
            if (!stack.isEmpty()) {
                Seasoning s = Seasoning.ofRaw(stack);
                kinds[i] = s == null ? -1 : s.ordinal();
            } else if (portions[i] == 0) {
                kinds[i] = -1;
            }
        }
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        DefaultedList<ItemStack> list = DefaultedList.ofSize(raw.size(), ItemStack.EMPTY);
        for (int i = 0; i < raw.size(); i++) {
            list.set(i, raw.getStack(i));
        }
        Inventories.writeNbt(nbt, list);
        nbt.putIntArray("Portions", portions);
        nbt.putIntArray("Kinds", kinds);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        DefaultedList<ItemStack> list = DefaultedList.ofSize(raw.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, list);
        loading = true;
        for (int i = 0; i < raw.size(); i++) {
            raw.setStack(i, list.get(i));
        }
        loading = false;
        int[] p = nbt.getIntArray("Portions");
        int[] k = nbt.getIntArray("Kinds");
        for (int i = 0; i < BOWLS; i++) {
            portions[i] = i < p.length ? p[i] : 0;
            // 老存档（碗固定位置时）没有 Kinds：第 i 格原来就是第 i 种调料
            kinds[i] = i < k.length ? k[i] : (portions[i] > 0 ? i : -1);
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


    @Override
    public Text getDisplayName() {
        return getCachedState().getBlock().getName();
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SauceStationScreenHandler(syncId, playerInventory, this);
    }
}
