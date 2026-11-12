package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import java.util.Arrays;
import java.util.List;
// mryt233 改（2026-11-11）：第三批 3G 存读档改走适配小包，键名与结构不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreReader;

import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.nbt.CompoundTag;
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
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public class SauceStationBlockEntity extends SyncedBlockEntity {
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
    /** mryt233：每碗持久化 ID；空字符串为空，未知 ID 保留。 */
    private final String[] kindIds = new String[BOWLS];
    /** 识别空槽被明确补货，避免无关槽变化抹掉未知 ID。 */
    private final boolean[] rawWasEmpty = new boolean[BOWLS];
    /** mryt233：记录各槽上次见到的原料，以区分玩家换料与配方重载导致的映射变化。 */
    private final ItemStack[] lastRaw = new ItemStack[BOWLS];
    /** 读存档时往格子里放东西，不要触发 changed（那时可能还在加载区块）。 */
    private boolean loading;

    public SauceStationBlockEntity(BlockPos worldPosition, BlockState state) {
        this(HotPotContent.SAUCE_STATION_BE, worldPosition, state);
    }

    SauceStationBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                            BlockPos worldPosition, BlockState state) {
        super(type, worldPosition, state);
        Arrays.fill(kindIds, "");
        Arrays.fill(rawWasEmpty, true);
        Arrays.fill(lastRaw, ItemStack.EMPTY);
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
        SeasoningDef def = kindDef(i);
        return def == null ? null : def.legacy();
    }

    public SeasoningDef kindDef(int i) {  // mryt233 改：旧公开入口保留枚举签名。
        ItemStack stack = raw.getItem(i);
        SeasoningDef saved = kindIds[i].isEmpty() ? null : SauceIds.def(kindIds[i]);
        if (!kindIds[i].isEmpty() && saved == null && available(i) > 0) return null;
        if (portions[i] > 0 && saved != null) {
            if (stack.isEmpty() || sameRawKind(lastRaw[i], stack)) return saved;
            SeasoningDef current = Seasonings.ofRaw(stack);
            return current != null ? current : saved;
        }
        if (!stack.isEmpty()) return Seasonings.ofRaw(stack);
        return null;
    }

    /** 会话索引：-1 空，-2 未知，不将未知回写为空。 */
    public int kindIndex(int i) {
        SeasoningDef def = kindDef(i);
        return slotIndex(kindIds[i], available(i), def == null ? -1 : Seasonings.indexOf(def),
            !kindIds[i].isEmpty() && SauceIds.def(kindIds[i]) == null);
    }

    static int slotIndex(String id, int available, int knownIndex, boolean unknown) {
        if (knownIndex >= 0) return knownIndex;
        return available > 0 && !id.isEmpty() && unknown ? -2 : -1;
    }

    /** 这个碗收不收这件原料：空碗什么调料的原料都收，有东西的碗只收同一种。 */
    public boolean accepts(int i, ItemStack stack) {
        SeasoningDef s = Seasonings.ofRaw(stack);
        SeasoningDef k = kindDef(i);
        return s != null && kindIndex(i) != -2 && (k == null || k == s);
    }

    /** 这个碗一共还能打几次（已化开的份数 + 格子里的原料 × 4）。 */
    public int available(int i) {
        return portions[i] + raw.getItem(i).getCount() * PORTIONS_PER_RAW;
    }

    /** 从第 i 个碗用掉一份；没货返回 false。 */
    private boolean take(int i) {
        if (portions[i] == 0) {
            ItemStack stack = raw.getItem(i);
            SeasoningDef next = stack.isEmpty() ? null : Seasonings.ofRaw(stack);
            if (next == null) {
                return false;
            }
            kindIds[i] = SauceIds.fullId(next);
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
        SeasoningDef s = kindDef(i);
        if (s == null || available(i) == 0) {
            player.displayClientMessage(Component.translatable(HotPotBlockEntity.key(
                kindIndex(i) == -2 ? "sauce.unknown" : "msg.bowl_empty")), true);
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean sauce = held.getItem() instanceof DippingSauceItem;
        if (sauce && DippingSauceItem.contents(held).has(SauceIds.fullId(s))) {
            player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.sauce_has"), Component.translatable(s.nameKey())), true);
            return;
        }
        if (!take(i)) {
            return;
        }
        if (sauce) {
            held.set(HotPotContent.SAUCE, DippingSauceItem.contents(held).with(SauceIds.fullId(s)));
            if (SauceIds.hasAllBuiltins(DippingSauceItem.contents(held).ids())) {   // mryt233 改（4E-2）：按内置 15 种算
                HotPotCriterion.fire(player, "sauce_full");
            }
        } else {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, DippingSauceItem.createWithId(SauceIds.fullId(s))));
        }
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    /** 存货变了：记下每个碗是哪种、存档、同步给客户端（台上的小碗跟着显示）。 */
    public void changed() {
        for (int i = 0; i < BOWLS; i++) {
            ItemStack stack = raw.getItem(i);
            boolean hadRaw = !rawWasEmpty[i];
            boolean hasRaw = !stack.isEmpty();
            boolean sameRaw = hasRaw && sameRawKind(lastRaw[i], stack);
            rawWasEmpty[i] = !hasRaw;
            lastRaw[i] = stack.copy();
            SeasoningDef saved = kindIds[i].isEmpty() ? null : SauceIds.def(kindIds[i]);
            SeasoningDef current = hasRaw ? Seasonings.ofRaw(stack) : null;
            kindIds[i] = updatedKindId(kindIds[i], portions[i], hadRaw, hasRaw,
                saved != null, current == null ? "" : SauceIds.fullId(current), sameRaw);
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean sameRawKind(ItemStack previous, ItemStack current) {
        return !previous.isEmpty() && !current.isEmpty() && ItemStack.isSameItemSameComponents(previous, current);
    }

    static String updatedKindId(String id, int portions, boolean hadRaw, boolean hasRaw,
                                boolean known, String newRawId) {
        return updatedKindId(id, portions, hadRaw, hasRaw, known, newRawId, false);
    }

    static String updatedKindId(String id, int portions, boolean hadRaw, boolean hasRaw,
                                boolean known, String newRawId, boolean sameRaw) {
        boolean newlyFilled = !hadRaw && hasRaw;
        if (!id.isEmpty() && !known && (!newlyFilled || !hasRaw || portions > 0)) return id;
        if (!id.isEmpty() && known && portions > 0 && hasRaw && (newRawId.isEmpty() || sameRaw)) return id;
        if (!hasRaw && portions > 0) return id;
        return hasRaw ? newRawId : "";
    }

    // mryt233 改（2026-11-11）：第三批 3G 存读档改走适配小包，键名、结构与兜底值不变。
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.remove("Kinds");
        BeIo.write(nbt, registries, w -> {
            NonNullList<ItemStack> list = NonNullList.withSize(raw.getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < raw.getContainerSize(); i++) {
                list.set(i, raw.getItem(i));
            }
            w.putItems(list);
            w.putIntArray("Portions", portions);
            // mryt233：固定九个字符串，旧 Kinds 留待读兼容至下个大版本。
            w.putStringList("KindIds", Arrays.asList(kindIds.clone()));
        });
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        BeIo.read(nbt, registries, r -> {
            NonNullList<ItemStack> list = NonNullList.withSize(raw.getContainerSize(), ItemStack.EMPTY);
            r.loadItems(list);
            loading = true;
            for (int i = 0; i < raw.getContainerSize(); i++) {
                ItemStack loaded = list.get(i);
                raw.setItem(i, loaded);
                rawWasEmpty[i] = loaded.isEmpty();
                lastRaw[i] = loaded.copy();
            }
            loading = false;
            readKindIds(r, portions, kindIds);
        });
    }


    /** mryt233：新键存在优先，坏类型/空列表不复活旧数据，且只保存 ID。 */
    static void readKindIds(StoreReader r, int[] portions, String[] target) {
        int[] p = r.getIntArray("Portions");
        int[] old = r.getIntArray("Kinds");
        List<String> ids = r.getStringList("KindIds");
        boolean hasIds = r.has("KindIds"), hasKinds = r.has("Kinds");
        for (int i = 0; i < BOWLS; i++) {
            portions[i] = i < p.length ? p[i] : 0;
            if (hasIds) target[i] = i < ids.size() ? ids.get(i) : "";
            else if (hasKinds) target[i] = i < old.length ? SauceIds.legacyId(old[i]) : (portions[i] > 0 ? SauceIds.legacyId(i) : "");
            else target[i] = portions[i] > 0 ? SauceIds.legacyId(i) : "";
        }
    }

    /** 保留公开入口，实际更新格式由公共基类提供。 */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return super.getUpdatePacket(); }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return super.getUpdateTag(registries); }
}