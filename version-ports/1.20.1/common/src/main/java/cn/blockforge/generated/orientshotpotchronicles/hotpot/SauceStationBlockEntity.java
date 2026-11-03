package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;

/**
 * 蘸料台的存货（r103，用户定；2026-10-23 用户改成碗不固定，调整）：台上 9 个小碗，每个碗对应补货界面的一个原料格。
 * 哪个碗装什么不固定——空碗（格子里没原料、也没剩份数）放哪种原料就变成哪种调料，同一种调料也可以放好几个碗；
 * 碗里还有东西时只能补同一种。1 个原料 = 4 份，最多 16 个 = 64 份；打一次用 1 份，份数用完时自动从格子里化开 1 个原料。
 * 每个碗装的什么、有没有货同步给客户端，由 client/SauceStationRenderer 画碗里的调料面。
 * 碗的编号 = 阶梯上的位置：从最上层到最下层、每层从左到右（见 SauceStationBlock.aimed）。
 * 作者：mryt233 */
public class SauceStationBlockEntity extends BlockEntity {
    public static final int BOWLS = 9;
    public static final int PORTIONS_PER_RAW = 4;
    public static final int MAX_RAW = 16;

    private final SimpleContainer raw = new SimpleContainer(BOWLS) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return accepts(slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return MAX_RAW;
        }
    };
    private final int[] portions = new int[BOWLS];
    /** 每个碗已经化开的份数是哪种调料（Seasoning 序号，-1 = 没有）。 */
    private final int[] kinds = new int[BOWLS];
    /** 读存档时往格子里放东西，不要触发 changed（那时可能还在加载区块）。 */
    private boolean loading;

    public SauceStationBlockEntity(BlockPos worldPosition, BlockState state) {
        super(HotPotContent.SAUCE_STATION_BE, worldPosition, state);
        java.util.Arrays.fill(kinds, -1);
        raw.addListener(inv -> {
            if (!loading) {
                changed();
            }
        });
    }

    public SimpleContainer rawInventory() {
        return raw;
    }

    public int portions(int i) {
        return portions[i];
    }

    /** 第 i 个碗装的是哪种调料：格子里有原料看原料，没原料但还剩份数看剩的；空碗返回 null。 */
    public Seasoning kind(int i) {
        ItemStack stack = raw.getItem(i);
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
        return portions[i] + raw.getItem(i).getCount() * PORTIONS_PER_RAW;
    }

    /** 从第 i 个碗用掉一份；没货返回 false。 */
    private boolean take(int i) {
        if (portions[i] == 0) {
            ItemStack stack = raw.getItem(i);
            if (stack.isEmpty()) {
                return false;
            }
            kinds[i] = Seasoning.ofRaw(stack).ordinal();
            stack.shrink(1);
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
    public void serve(Player player, InteractionHand hand, int i) {
        Seasoning s = kind(i);
        if (s == null || available(i) == 0) {
            player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.bowl_empty")), true);
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean sauce = held.getItem() instanceof DippingSauceItem;
        if (sauce && DippingSauceItem.contents(held).has(s)) {
            player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.sauce_has"), Component.translatable(s.nameKey())), true);
            return;
        }
        if (!take(i)) {
            return;
        }
        if (sauce) {
            cn.blockforge.orient.framework.compat.ItemData.set(held, HotPotContent.SAUCE, DippingSauceItem.contents(held).with(s));
            if (DippingSauceItem.contents(held).mask() == (1 << Seasoning.ALL.length) - 1) {
                HotPotCriterion.fire(player, "sauce_full");
            }
        } else {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, DippingSauceItem.create(s)));
        }
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    /** 存货变了：记下每个碗是哪种、存档、同步给客户端（台上的小碗跟着显示）。 */
    public void changed() {
        for (int i = 0; i < BOWLS; i++) {
            ItemStack stack = raw.getItem(i);
            if (!stack.isEmpty()) {
                Seasoning s = Seasoning.ofRaw(stack);
                kinds[i] = s == null ? -1 : s.ordinal();
            } else if (portions[i] == 0) {
                kinds[i] = -1;
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        NonNullList<ItemStack> list = NonNullList.withSize(raw.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < raw.getContainerSize(); i++) {
            list.set(i, raw.getItem(i));
        }
        ContainerHelper.saveAllItems(nbt, list);
        nbt.putIntArray("Portions", portions);
        nbt.putIntArray("Kinds", kinds);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        NonNullList<ItemStack> list = NonNullList.withSize(raw.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, list);
        loading = true;
        for (int i = 0; i < raw.getContainerSize(); i++) {
            raw.setItem(i, list.get(i));
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
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

}